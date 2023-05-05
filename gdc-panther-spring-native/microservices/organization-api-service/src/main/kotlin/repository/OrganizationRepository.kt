/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.repository

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.core.io.IOContext
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.dataformat.yaml.util.StringQuotingChecker
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.gooddata.panther.organizationapi.config.OrganizationRepositoryProperties
import com.gooddata.panther.organizationapi.dto.OrgDefinition
import com.gooddata.panther.organizationapi.error.OrganizationNotFoundException
import mu.KotlinLogging
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.filter.PathFilter
import org.eclipse.jgit.treewalk.filter.TreeFilter
import org.yaml.snakeyaml.DumperOptions
import java.io.File
import java.io.FilenameFilter
import java.io.Writer
import java.util.Calendar
import kotlin.io.path.createTempDirectory
import com.gooddata.panther.organizationapi.error.GitException as GdcGitException

private val logger = KotlinLogging.logger { }

/**
 * The exception is thrown when a push wasn't successful.
 */
class PushException(message: String?) : TransportException(
    String.format("Could not push the local repository: '%s'", message)
)

class OrganizationRepository(
    private val organizationRepositoryProperties: OrganizationRepositoryProperties
) : AutoCloseable {
    private val credentialProvider = UsernamePasswordCredentialsProvider(
        "PRIVATE_TOKEN",
        organizationRepositoryProperties.token
    )
    private val git: Git = gitClone()
    private val objectReader: ObjectReader = git.repository.newObjectReader()

    fun createOrganization(organization: OrgDefinition, cluster: String) {
        logger.info(
            "action=create_organization_repository status=START organization_id={} cluster={} deployment={}",
            organization.spec.id,
            cluster,
            organization.metadata.namespace,
        )
        try {
            gitCommitChanges(
                prepareOrganizationFile(organization, cluster),
                organization,
                cluster
            )
            push()
            logger.info(
                "action=create_organization_repository status=FINISH organization_id={} cluster={} deployment={}",
                organization.spec.id,
                cluster,
                organization.metadata.namespace,
            )
        } catch (e: GitAPIException) {
            logger.error(
                "action=create_organization_repository status=ERROR message={} organization_id={} cluster={} " +
                    "deployment={}",
                e.message,
                organization.spec.id,
                cluster,
                organization.metadata.namespace,
            )
            throw GdcGitException()
        }
    }

    fun createOrganizationsAll(organizations: List<OrgDefinition>, cluster: String) {
        if (organizations.isEmpty()) return

        organizations.forEach { gitCommitChanges(prepareOrganizationFile(it, cluster), it, cluster) }
        git.push().setCredentialsProvider(credentialProvider).call()

        logger.info(
            "action=prepare_organization_bulk organizations=[{}] message=organization_pushed_to_git",
            organizations.joinToString { it.metadata.name },
        )
    }

    fun listOrganizations(cluster: String, deployment: String): List<String> {
        val path = File(git.repository.workTree, "$cluster/$deployment")
        val filter = FilenameFilter { _: File?, name: String -> name.endsWith(".yaml") }
        return path.list(filter)?.map {
            mapper.readValue(File("${path.absolutePath}/$it"), OrgDefinition::class.java).metadata.name
        } ?: listOf()
    }

    /**
     * List names of all deleted organizations for last [lastCommitDay] days.
     *
     * @param cluster cluster on which deleted organizations are looked for
     * @param deployment deployment on which deleted organizations are looked for
     * @param lastCommitDay number of days to past where start to look for deleted organizations
     * @return list of names of all deleted organizations for last [lastCommitDay] days
     */
    fun listDeletedOrganizations(
        cluster: String,
        deployment: String,
        lastCommitDay: Int = 20
    ): List<String> {
        val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -lastCommitDay) }
        val revCommits = git.log()
            .setRevFilter(CommitTimeRevFilter.after(calendar.time))
            .call()
            .reversed()
        return getDeletedOrganizationNames(revCommits, "$cluster/$deployment/")
    }

    /**
     * Compare each commit with the commit next in the commits tree, and return deleted organization names according
     * to delete change type from the diff
     *
     * @param revCommits revision commits sorted from oldest to newest
     * @param pathPrefix path prefix of deleted organization file
     * @return list of names of all deleted organizations in given list [revCommits] on path defined with [pathPrefix]
     */
    private fun getDeletedOrganizationNames(revCommits: List<RevCommit>, pathPrefix: String): List<String> =
        revCommits.zip(revCommits.drop(1)).map { (olderCommit, newerCommit) ->
            compareCommits(olderCommit, newerCommit, PathFilter.create(pathPrefix))
                .filter { it.changeType == DiffEntry.ChangeType.DELETE }
                .map { it.oldPath.removePrefix(pathPrefix).removeSuffix(YAML_SUFFIX) }
        }.flatten()

    /**
     * Compare [first] commit with [second] commit and returns diffs of files on path given with [pathFilter]
     */
    private fun compareCommits(
        first: RevCommit,
        second: RevCommit,
        pathFilter: TreeFilter = PathFilter.ALL,
    ): List<DiffEntry> {
        val firstTree = CanonicalTreeParser(null, objectReader, first.tree.id)
        val secondTree = CanonicalTreeParser(null, objectReader, second.tree.id)
        return git.diff()
            .setOldTree(firstTree)
            .setNewTree(secondTree)
            .setPathFilter(pathFilter)
            .call()
    }

    /**
     * Removes organization through the git command, then commit and push if the removed status is higher than zero.
     *
     * @param organizationId organization to remove
     * @param cluster to which cluster org belongs
     * @param deployment to which deployment org belongs
     * @return unit if removed, otherwise throw exception
     */
    fun deleteOrganization(organizationId: String, cluster: String, deployment: String) {
        val file = destination(cluster, deployment, organizationId)
        val relativePath = file.relativeTo(git.repository.workTree).toString()

        if (file.exists()) {
            logger.info(
                "action=delete_organization_repository status=START organization_id={} path={}",
                organizationId, relativePath
            )
            try {
                // We should be able to delete file without issue whereas file was located on working tree repository
                gitRemove(relativePath)
                commit("[$cluster/$deployment] Remove organization $organizationId")
                verifyPush(organizationId)
                logger.info(
                    "action=delete_organization_repository status=FINISH " +
                        "message=deleted_organization_from_repository organization_id={} path={}",
                    organizationId, relativePath
                )
            } catch (e: GitAPIException) {
                logger.error(
                    "action=delete_organization_repository status=ERROR message={} organization_id={} cluster={} " +
                        "deployment={} path={}",
                    e.message,
                    organizationId,
                    cluster,
                    deployment,
                    relativePath
                )
                throw GdcGitException()
            }
        } else {
            val message = "Organization not found"
            logger.error(
                "action=delete_organization_repository status=ERROR message=$message organization_id={} " +
                    "cluster={} deployment={} path={}",
                organizationId,
                cluster,
                deployment,
                relativePath
            )
            throw OrganizationNotFoundException(
                "$message $cluster/$deployment/$organizationId. " +
                    "It could be deleted from the repository, or you have passed the wrong organization id."
            )
        }
    }

    /**
     * Do git push and verify if a push is successful.
     *
     * @param organizationId organization to log
     */
    private fun verifyPush(organizationId: String) {
        for (pushResult in push()) {
            for (remoteUpdate in pushResult.remoteUpdates) {
                logger.info(
                    "action=delete_organization_repository method=git_push " +
                        "status={} message={} organization_id={}",
                    remoteUpdate.status,
                    remoteUpdate.message,
                    organizationId
                )
                if (remoteUpdate.status != RemoteRefUpdate.Status.OK) {
                    throw PushException(pushResult.messages)
                }
            }
        }
    }

    private fun push() = git.push().setCredentialsProvider(credentialProvider).call()

    private fun gitRemove(relativePath: String) = git.rm().addFilepattern(relativePath).call()

    private fun commit(message: String) {
        val commit = git.commit()
        commit.message = message
        commit.committer = PersonIdent(
            organizationRepositoryProperties.committerName,
            organizationRepositoryProperties.committerEmail
        )
        commit.call()
    }

    private fun gitClone(): Git {
        val gitFolder = createTempDirectory(directory = organizationRepositoryProperties.directory).toFile()
        logger.info("action=repository_initialization status=START path={}", gitFolder)
        val repository = Git.cloneRepository()
            .setURI(organizationRepositoryProperties.path)
            .setBranch(organizationRepositoryProperties.branch)
            .setDirectory(gitFolder)
            .setCredentialsProvider(credentialProvider)
            .call()
        require(repository.repository.workTree.listFiles().size > 1) { "The target repository is empty" }
        logger.info("action=repository_initialization status=FINISHED path={}", gitFolder)
        return repository
    }

    override fun close() {
        val gitFolder = git.repository.workTree
        gitFolder.deleteRecursively()
        logger.info("action=repository_destroy path={}", gitFolder.path)
    }

    private fun destination(cluster: String, deployment: String, organizationId: String): File {
        return File(git.repository.workTree, "$cluster/$deployment/$organizationId.yaml")
    }

    private fun prepareOrganizationFile(organization: OrgDefinition, cluster: String): File {

        val file = destination(
            cluster,
            organization.metadata.namespace,
            organization.spec.id
        )

        require(file.parentFile.exists()) { "Deployment does not exits" }

        mapper.writeValue(file, organization)
        logger.info("action=prepare_organization_for_commit organization_id={}", organization.spec.id)
        return file
    }

    private fun gitCommitChanges(file: File, organization: OrgDefinition, cluster: String) {
        val relativePath = file.relativeTo(git.repository.workTree).toString()
        git.add().addFilepattern(relativePath).call()
        logger.info(
            "action=prepare_organization organization_id={} path={} method=commit",
            organization.spec.id, relativePath
        )

        val status = git.status().call()
        if (status.added.size > 0) {
            commit("[$cluster/${organization.metadata.namespace}] Add organization ${organization.spec.id}")
        } else {
            logger.warn(
                "action=prepare_organization message=no_changes_on_organization organization_id={} path={}",
                organization.spec.id,
                relativePath
            )
        }
    }

    companion object {
        private const val YAML_SUFFIX = ".yaml"

        private val mapper = prepareYAMLMapper()

        private fun prepareYAMLMapper(): ObjectMapper {
            val organizationYamlFactory = object : YAMLFactory() {
                override fun _createGenerator(out: Writer, ctxt: IOContext) =
                    OrgYAMLGenerator(ctxt, _generatorFeatures, _yamlGeneratorFeatures, _objectCodec, out, _version)
            }

            return YAMLMapper(organizationYamlFactory)
                .registerKotlinModule()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }
}

/* Own implementation of YAMLGenerator is needed because of redefinition output format of YAML to make yamllint happy
* In this case, it is needed to make 4 spaces as indentation in front of members of an array.
* indent - default indentation
* indicator indent - number of spaces before `-`
* indentWithIndicator - needs to be set to true otherwise the generator will add new lines after the indicator */
private class OrgYAMLGenerator(
    ctx: IOContext,
    jsonFeatures: Int,
    yamlFeatures: Int,
    codec: ObjectCodec,
    out: Writer,
    version: DumperOptions.Version?
) : YAMLGenerator(ctx, jsonFeatures, yamlFeatures, StringQuotingChecker.Default(), codec, out, version) {
    override fun buildDumperOptions(
        jsonFeatures: Int,
        yamlFeatures: Int,
        version: DumperOptions.Version?
    ): DumperOptions = super.buildDumperOptions(jsonFeatures, yamlFeatures, version).apply {
        indicatorIndent = 2
        indent = 2
        indentWithIndicator = true
    }
}
