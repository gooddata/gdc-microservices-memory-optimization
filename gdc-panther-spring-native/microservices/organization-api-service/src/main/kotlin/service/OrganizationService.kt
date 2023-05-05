/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.service

import com.gooddata.panther.common.exception.BadRequestException
import com.gooddata.panther.common.metric.MetricLogger
import com.gooddata.panther.organizationapi.config.OrganizationProperties
import com.gooddata.panther.organizationapi.config.OrganizationRepositoryProperties
import com.gooddata.panther.organizationapi.config.OrganizationServiceConfig
import com.gooddata.panther.organizationapi.dto.DeploymentPropertiesDto
import com.gooddata.panther.organizationapi.dto.OrgDefinition
import com.gooddata.panther.organizationapi.dto.OrganizationDto
import com.gooddata.panther.organizationapi.dto.TierOrganizationDto
import com.gooddata.panther.organizationapi.dto.SynchronizeRequestDto
import com.gooddata.panther.organizationapi.dto.TierType
import com.gooddata.panther.organizationapi.logging.MetricCounterType
import com.gooddata.panther.organizationapi.logging.MetricTimerType
import com.gooddata.panther.organizationapi.repository.OrganizationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException

private val logger = KotlinLogging.logger { }

@Service
class OrganizationService(
    val organizationServiceConfig: OrganizationServiceConfig,
    val organizationRepositoryProperties: OrganizationRepositoryProperties,
    val organizationProperties: OrganizationProperties,
    val kubernetesService: KubernetesService,
    val organizationTierPropertyService: OrganizationTierPropertyService,
    val metricLogger: MetricLogger
) {
    suspend fun create(organization: OrganizationDto): OrganizationDto {
        logger.info(
            "action=create_organization organization_id={} method=validate_organization_definition",
            organization.id
        )

        val entitlementsWithDefaults = coverDefaultEntitlements(organization.entitlementsConverted)
        checkMandatoryEntitlements(entitlementsWithDefaults)
        val cleanEntitlements = removeCollisionEntitlements(entitlementsWithDefaults)

        logger.info(
            "action=create_organization organization_id={} method=create_organization",
            organization.id
        )

        withContext(Dispatchers.IO) {
            metricLogger.durationWithCounter(
                MetricTimerType.CREATE_ORG_DURATION,
                MetricCounterType.CREATE_ORG
            ) {
                createOrganizationInServices(organization, cleanEntitlements)
            }
        }
        return OrganizationDto(
            organization.id,
            organization.name,
            organization.hostname,
            organization.adminUserToken,
            organization.adminUserTokenSecret,
            organization.deploymentProperties,
            cleanEntitlements,
            organization.tls,
            organization.orgAnnotations,
            organization.ingressAnnotations,
            organization.oauthProvider
        )
    }

    suspend fun create(tierOrganizationDto: TierOrganizationDto, tier: String): OrganizationDto {
        val tierType: TierType = try {
            TierType.valueOf(tier.uppercase())
        } catch (e: IllegalArgumentException) {
            logger.error("action=create_organization status=error message=Incorrect tier type: {}", tier)
            throw BadRequestException(
                message = "Incorrect tier type: $tier"
            )
        }
        return create(organizationTierPropertyService.toOrganizationDto(tierOrganizationDto, tierType))
    }

    suspend fun synchronizeOrganizations(synchronize: SynchronizeRequestDto) = if (hasManagedDeployments()) {
        OrganizationRepository(organizationRepositoryProperties).use { repository ->
            organizationServiceConfig.controlledNamespaces.associateWith { namespace ->
                synchronizeOrganizations(namespace, synchronize, repository)
            }
        }
    } else emptyMap()

    private fun synchronizeOrganizations(
        namespace: String,
        synchronize: SynchronizeRequestDto,
        repository: OrganizationRepository
    ): List<String> = getSynchronizeOrganizations(namespace, repository).let { synchronizeOrganizations ->
        if (!synchronize.dryRun) {
            val cleanOrganizationDefinitions = kubernetesService.cleanOrgDefinitions(synchronizeOrganizations)
            repository.createOrganizationsAll(cleanOrganizationDefinitions, organizationServiceConfig.managedCluster)
        }
        synchronizeOrganizations.map { orgDefinition -> orgDefinition.metadata.name }
    }

    private fun getSynchronizeOrganizations(
        namespace: String,
        repository: OrganizationRepository,
    ): List<OrgDefinition> =
        repository.listOrganizations(organizationServiceConfig.managedCluster, namespace).plus(
            repository.listDeletedOrganizations(organizationServiceConfig.managedCluster, namespace)
        ).let { storedAndDeletedOrganizations ->
            kubernetesService.listOrganizations(namespace, except = storedAndDeletedOrganizations)
        }

    /**
     * Deletes organization in both places, in the git repository and the k8s.
     */
    suspend fun deleteOrganization(orgId: String, deploymentProperties: DeploymentPropertiesDto): Boolean {
        logger.info(
            "action=delete_organization organization_id={} cluster={} deployment={}",
            orgId,
            deploymentProperties.cluster,
            deploymentProperties.deployment
        )

        withContext(Dispatchers.IO) {
            metricLogger.durationWithCounter(
                MetricTimerType.DELETE_ORG_DURATION,
                MetricCounterType.DELETE_ORG
            ) {
                // Delete from GitLab, and then from k8s it will be deleted by the Flux.
                deleteOrgInRepository(
                    orgId,
                    deploymentProperties.cluster,
                    deploymentProperties.deployment
                )
            }
        }
        return true
    }

    private fun hasManagedDeployments() = with(organizationServiceConfig) {
        managedCluster.isNotBlank() && controlledNamespaces.isNotEmpty()
    }

    private fun checkMandatoryEntitlements(entitlements: Map<String, String>) {
        require(
            organizationProperties.mandatoryEntitlements.none { m ->
                !entitlements.keys.contains(m)
            }
        ) { "Some of mandatory entitlements are missing" }
    }

    private fun shouldCreateOrgInKubernetes(deployment: DeploymentPropertiesDto) =
        organizationServiceConfig.managedCluster == deployment.cluster &&
            organizationServiceConfig.controlledNamespaces.contains(deployment.deployment)

    private fun coverDefaultEntitlements(entitlements: Map<String, String>): Map<String, String> {
        return organizationProperties.defaultEntitlements.plus(entitlements).toMap()
    }

    private fun removeCollisionEntitlements(entitlements: Map<String, String>): Map<String, String> {
        return entitlements.filter {
            when (it.key) {
                "USER_COUNT" -> !entitlements.containsKey("UNLIMITED_USERS")
                "WORKSPACE_COUNT" -> !entitlements.containsKey("UNLIMITED_WORKSPACES")
                else -> true
            }
        }
    }

    private fun deleteOrgInRepository(organizationId: String, cluster: String, deployment: String) {
        metricLogger.durationWithCounter(
            MetricTimerType.DELETE_ORG_REPOSITORY_DURATION,
            MetricCounterType.DELETE_ORG_REPOSITORY
        ) {
            OrganizationRepository(organizationRepositoryProperties).use { repository ->
                repository.deleteOrganization(organizationId, cluster, deployment)
            }
        }
    }

    private fun createOrganizationInKubernetes(definition: OrgDefinition) {
        metricLogger.durationWithCounter(MetricTimerType.CREATE_ORG_K8S_DURATION,
            MetricCounterType.CREATE_ORG_K8S) {
            kubernetesService.createOrganization(definition)
        }
    }

    private fun createOrganizationInRepository(
        definition: OrgDefinition,
        deployment: DeploymentPropertiesDto
    ) {
        metricLogger.durationWithCounter(
            MetricTimerType.CREATE_ORG_REPOSITORY_DURATION,
            MetricCounterType.CREATE_ORG_REPOSITORY
        ) {
            OrganizationRepository(organizationRepositoryProperties).use { repository ->
                repository.createOrganization(
                    organization = definition,
                    cluster = deployment.cluster
                )
            }
        }
    }

    private fun createOrganizationInServices(organization: OrganizationDto, cleanEntitlements: Map<String, String>) {
        val definition = OrgDefinition.build(organization, cleanEntitlements)
        if (organization.dryRun!!) {
            logger.info("action=build_organization_definition organization_definition={}", definition)
        } else {
            val directKubernetes = shouldCreateOrgInKubernetes(organization.deploymentProperties)

            if (directKubernetes) createOrganizationInKubernetes(definition)
            createOrganizationInRepository(definition, organization.deploymentProperties)
        }
    }
}
