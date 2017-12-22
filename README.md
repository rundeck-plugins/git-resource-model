# Rundeck GIT Resource Model

This is a plugin for Rundeck > [2.10.0](http://rundeck.org/) that uses Git to store resources model file (based on [Jgit](https://www.eclipse.org/jgit/)).


## Build

Run the following command to built the jar file:

```
./gradlew clean build
```


## Install

Copy the `rundeck-git-plugin-x.y.x.jar` file to the `$RDECK_BASE/libext/` directory inside your Rundeck installation.


## Configuration

You need to set up the following options to use the plugin:

![](settings.png)

### Repo Settings

* **Base Directory**: Directory for checkout
* **Git URL**: Checkout URL.
    See [git-clone](https://www.kernel.org/pub/software/scm/git/docs/git-clone.html)
    specifically the [GIT URLS](https://www.kernel.org/pub/software/scm/git/docs/git-clone.html#URLS) section.
    Some examples:
    * `ssh://[user@]host.xz[:port]/path/to/repo.git/`
    * `git://host.xz[:port]/path/to/repo.git/`
    * `http[s]://host.xz[:port]/path/to/repo.git/`
    * `ftp[s]://host.xz[:port]/path/to/repo.git/`
    * `rsync://host.xz/path/to/repo.git/`

* **Branch**: Checkout branch
* **Resource model File**: Resource model file inside the github repo. This is the file that will be added to Rundeck resource model.
* **File Format**:  File format of the resource model, it could be xml, yaml, json
* **Writable**: Allow to write the remote file

### Authentication

* **Git Password**: Password to authenticate remotely
* **SSH: Strict Host Key Checking**: Use strict host key checking.
If `yes`, require remote host SSH key is defined in the `~/.ssh/known_hosts` file, otherwise do not verify.
* **SSH Key Path**: SSH Key Path to authenticate

The first method of authentication is the private key.If the private key is not defined, it will take the password. 

* The primary key will work with SSH protocol on the Git URL. 
* The password will work with http/https protocol on the Git URL (the most of the case, the username is needed on the URI, eg: `http[s]://username@host.xz[:port]/path/to/repo.git/`  when you use password authentication)

## Limitations

* The plugin needs to clone the full repo on the local directory path (Base Directory option) to get the file that will be added to the resource model.
* Any time that you edit the nodes on the GUI, the commit will be perfomed with the message `Edit node from GUI`  (it is not editable)

