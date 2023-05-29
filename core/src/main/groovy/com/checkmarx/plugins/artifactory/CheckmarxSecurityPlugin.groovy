package com.checkmarx.plugins.artifactory

import groovy.transform.Field
import org.artifactory.fs.ItemInfo
import org.artifactory.repo.RepoPath
import org.artifactory.request.Request

@Field CheckmarxPlugin checkmarxPlugin

initialize()

private void initialize() {
  log.info("Initializing CheckmarxSecurityPlugin...")

  final File pluginsDirectory = ctx.artifactoryHome.pluginsDir
  checkmarxPlugin = new CheckmarxPlugin(repositories, pluginsDirectory)

  log.info("Initialization of CheckmarxSecurityPlugin completed")
}

executions {
  checkmarxSecurityReload(httpMethod: "POST") { params ->
    initialize()
  }
}

download {
  beforeDownload { Request request, RepoPath repoPath ->
    checkmarxPlugin.handleBeforeDownloadEvent(repoPath,request)
  }
}

storage {
  afterPropertyCreate { ItemInfo itemInfo, String propertyName, String[] propertyValues ->
    checkmarxPlugin.handleAfterPropertyCreateEvent(security.currentUser(), itemInfo, propertyName, propertyValues)
  }
}
