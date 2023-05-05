/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.service

import com.gooddata.panther.organizationapi.Kubernetes.Organization.GROUP
import com.gooddata.panther.organizationapi.Kubernetes.Organization.OBJECT
import com.gooddata.panther.organizationapi.Kubernetes.Organization.VERSION
import com.gooddata.panther.organizationapi.dto.OrgDefinition
import com.gooddata.panther.organizationapi.error.K8sException
import com.google.gson.Gson
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.JSON
import io.kubernetes.client.openapi.apis.CustomObjectsApi
import io.kubernetes.client.util.Config
import jakarta.inject.Singleton
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }
private val KUBERNETES_ANNOTATION_PREFIXES = listOf("kopf.", "kubectl.")

@Singleton
class KubernetesService {
    private val client: ApiClient = Config.defaultClient()
    private val api: CustomObjectsApi
    private val gson: Gson = JSON().gson

    init {
        Configuration.setDefaultApiClient(client)
        api = CustomObjectsApi(client)
    }

    fun createOrganization(organization: OrgDefinition) {
        logger.info(
            "action=create_organization_k8s status=START organization_id={} namespace={}",
            organization.spec.id, organization.metadata.namespace
        )
        try {
            api.createNamespacedCustomObject(
                GROUP,
                VERSION,
                organization.metadata.namespace,
                OBJECT,
                organization,
                null,
                null,
                "kustomize-controller"
            )
            logger.info(
                "action=create_organization_k8s status=FINISH message=organization_pushed_to_k8s " +
                    "organization_id={} namespace={}",
                organization.spec.id, organization.metadata.namespace
            )
        } catch (ex: ApiException) {
            logger.error(
                "action=create_organization_k8s status=ERROR message=${apiExceptionToString(ex)} " +
                    "organization_id={} namespace={}",
                organization.spec.id, organization.metadata.namespace
            )
            throw K8sException()
        }
    }

    fun listOrganizations(namespace: String, except: List<String> = listOf()): List<OrgDefinition> {
        val organizationListRaw = listOrganizationsRaw(namespace)

        return organizationListRaw.mapNotNull {
            kubernetesObjectToOrgDefinition(it).takeIf { organization ->
                !except.contains(organization.metadata.name)
            }
        }
    }

    fun cleanOrgDefinitions(organizations: List<OrgDefinition>): List<OrgDefinition> {
        return organizations.map { filterOutKopfMetadataAnnotations(it) }
    }

    private fun filterOutKopfMetadataAnnotations(organization: OrgDefinition): OrgDefinition {
        if (organization.metadata.annotations.isNullOrEmpty()) return organization

        return OrgDefinition.build(
            organization,
            organization.metadata.annotations.filterKeys { key ->
                KUBERNETES_ANNOTATION_PREFIXES.none { key.startsWith(it) }
            }
        )
    }

    private fun apiExceptionToString(ex: ApiException): String {
        return "${ex.message} responseBody=${ex.responseBody} code=${ex.code} responseHeaders=" +
            ex.responseHeaders.entries.joinToString(separator = "; ") {
                "${it.key}=${it.value.joinToString { value -> value }}"
            }
    }

    private fun kubernetesObjectToOrgDefinition(kubernetesObject: Any?): OrgDefinition {
        return gson.fromJson(gson.toJsonTree(kubernetesObject), OrgDefinition::class.java)
    }

    private fun listOrganizationsRaw(namespace: String) =
        listOrganizationsCustomObjectsMap(namespace)["items"] as List<*>

    private fun listOrganizationsCustomObjectsMap(namespace: String) = api.listNamespacedCustomObject(
        GROUP,
        VERSION,
        namespace,
        OBJECT,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    ) as Map<*, *>
}
