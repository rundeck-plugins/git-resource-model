package com.rundeck.plugin

import com.rundeck.plugin.util.PluginSshSessionFactory
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.PullResult
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.util.FileUtils
import org.rundeck.app.spi.Services
import com.dtolabs.rundeck.core.storage.keys.KeyStorageTree
import com.rundeck.plugin.util.GitPluginUtil

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by luistoledo on 12/20/17.
 */
class GitManager {

    private static final Logger logger = LoggerFactory.getLogger(GitManager.class);

    public static final String REMOTE_NAME = "origin"
    Git git
    String branch
    String fileName
    Repository repo
    String strictHostKeyChecking
    String sshPrivateKeyPath
    String gitPassword
    String gitURL
    String gitPasswordKeyStoragePath
    String gitSshKeyKeyStoragePath
    Services services

    GitManager(Properties configuration, Services services) {
        this.gitURL=configuration.getProperty(GitResourceModelFactory.GIT_URL)
        this.branch = configuration.getProperty(GitResourceModelFactory.GIT_BRANCH)
        this.fileName=configuration.getProperty(GitResourceModelFactory.GIT_FILE)
        this.strictHostKeyChecking=configuration.getProperty(GitResourceModelFactory.GIT_HOSTKEY_CHECKING)
        sshPrivateKeyPath=configuration.getProperty(GitResourceModelFactory.GIT_KEY_STORAGE)
        gitPassword=configuration.getProperty(GitResourceModelFactory.GIT_PASSWORD_STORAGE)
        this.gitPasswordKeyStoragePath=configuration.getProperty(GitResourceModelFactory.GIT_PASSWORD_KEY_STORAGE_PATH)
        this.gitSshKeyKeyStoragePath=configuration.getProperty(GitResourceModelFactory.GIT_SSH_KEY_KEY_STORAGE_PATH)
        this.services = services
    }

    Map<String, String> getSshConfig() {
        def config = [:]

        if (strictHostKeyChecking in ['yes', 'no']) {
            config['StrictHostKeyChecking'] = strictHostKeyChecking
        }
        config
    }

    protected void cloneOrCreate(File base, String url) throws Exception {
        if (base.isDirectory() && new File(base, ".git").isDirectory()) {
            def arepo = new FileRepositoryBuilder().setGitDir(new File(base, ".git")).setWorkTree(base).build()
            def agit = new Git(arepo)

            def config = agit.getRepository().getConfig()
            def found = config.getString("remote", REMOTE_NAME, "url")

            def needsClone=false;

            if (found != url) {
                logger.debug("url differs, re-cloning ${found}!=${url}")
                needsClone = true
            }else if (agit.repository.getFullBranch() != "refs/heads/$branch") {
                logger.debug("branch differs, re-cloning")
                needsClone = true
            }

            if(needsClone){
                removeWorkdir(base)
                performClone(base, url)
                return
            }

            git = agit
            repo = arepo
        } else {
            performClone(base, url)
        }
    }


    private void removeWorkdir(File base) {
        FileUtils.delete(base, FileUtils.RECURSIVE)
    }


    private void performClone(File base, String url) {

        def cloneCommand = Git.cloneRepository().
                setBranch(this.branch).
                setRemote(REMOTE_NAME).
                setDirectory(base).
                setURI(url)
        setupTransportAuthentication(sshConfig, cloneCommand, url)
        try {
            git = cloneCommand.call()
        } catch (Exception e) {
            logger.debug("Failed cloning the repository from ${url}: ${e.message}", e)
            throw new Exception("Failed cloning the repository from ${url}: ${e.message}", e)
        }
        repo = git.getRepository()
    }

    void setupTransportAuthentication(
            Map<String, String> sshConfig,
            TransportCommand command,
            String url = null) throws Exception{
        if (!url) {
            url = command.repository.config.getString('remote', REMOTE_NAME, 'url')
        }
        if (!url) {
            throw new NullPointerException("url for remote was not set")
        }

        URIish u = new URIish(url);
        logger.debug("transport url ${u}, scheme ${u.scheme}, user ${u.user}")

        if ((u.scheme == null || u.scheme == 'ssh') && u.user && sshPrivateKeyPath) {
            logger.debug("using ssh private key path ${sshPrivateKeyPath}")

            Path path = Paths.get(sshPrivateKeyPath);
            byte[] keyData = Files.readAllBytes(path);
            def factory = new PluginSshSessionFactory(keyData)
            factory.sshConfig = sshConfig
            command.setTransportConfigCallback(factory)
        } else if ((u.scheme == null || u.scheme == 'ssh') && u.user && gitSshKeyKeyStoragePath) {
            KeyStorageTree keyStorage = services.getService(KeyStorageTree.class)
            String key = GitPluginUtil.getPasswordFromKeyStorage(gitSshKeyKeyStoragePath, keyStorage)
            byte[] keyData = key.getBytes();
            def factory = new PluginSshSessionFactory(keyData)
            factory.sshConfig = sshConfig
            command.setTransportConfigCallback(factory)
        } else if (u.user && gitPasswordKeyStoragePath) {
            KeyStorageTree keyStorage = services.getService(KeyStorageTree.class)
            String key = GitPluginUtil.getPasswordFromKeyStorage(gitPasswordKeyStoragePath, keyStorage)
            command.setCredentialsProvider(new UsernamePasswordCredentialsProvider(u.user, key))
        } else if (u.user && gitPassword) {
            logger.debug("using password")

            if (null != gitPassword && gitPassword.length() > 0) {
                command.setCredentialsProvider(new UsernamePasswordCredentialsProvider(u.user, gitPassword))
            }
        }
    }

    PullResult gitPull(Git git1 = null) {
        def pullCommand = (git1 ?: git).pull().setRemote(REMOTE_NAME).setRemoteBranchName(branch)
        setupTransportAuthentication(sshConfig,pullCommand)
        pullCommand.call()
    }

    def gitCommitAndPush(){

        ////PERFORM COMMIT
        git.add()
                .addFilepattern(this.fileName)
                .call();

        // and then commit the changes
        //TODO: define a custom (or imput name) for the commit
        git.commit()
                .setMessage("Edit node from GUI")
                .call();

        println("Committed file " + this.fileName + " to repository at " + repo.getDirectory());

        /// PERFORM PUSH
        def pushb = git.push()
        pushb.setRemote(REMOTE_NAME)
        pushb.add(branch)
        setupTransportAuthentication(sshConfig, pushb)

        def push
        try {
            push = pushb.call()
        } catch (Exception e) {
            logger.debug("Failed push to remote: ${e.message}", e)
            throw new Exception("Failed push to remote: ${e.message}", e)
        }
        def sb = new StringBuilder()
        def updates = (push*.remoteUpdates).flatten()
        updates.each {
            sb.append it.toString()
        }

        String message=""
        def failedUpdates = updates.findAll { it.status != RemoteRefUpdate.Status.OK }
        if (failedUpdates) {
            message = "Some updates failed: " + failedUpdates
        } else {
            message = "Remote push result: OK."
        }

        logger.debug(message)

    }

    InputStream getFile(String localPath) {

        File base = new File(localPath)

        if(!base){
            base.mkdir()
        }

        //start the new repo, if the repo is create nothing will be done
        this.cloneOrCreate(base, gitURL)

        //always perform a pull
        //TODO: check if it is needed check for the repo status
        //and perform the pull when the last commit is different to the last commit on the local repo
        this.gitPull()

        File file = new File(localPath+"/"+fileName)
        return file.newInputStream()

    }


}
