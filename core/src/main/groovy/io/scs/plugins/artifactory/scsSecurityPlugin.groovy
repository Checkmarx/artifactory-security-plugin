package io.scs.plugins.artifactory

import groovy.transform.Field
import org.artifactory.fs.ItemInfo
import org.artifactory.repo.RepoPath
import org.artifactory.request.Request

@Field ScSPlugin scsPlugin

initialize()

private void initialize() {
  log.info("Initializing scsSecurityPlugin...")

  final File pluginsDirectory = ctx.artifactoryHome.pluginsDir
  scsPlugin = new ScSPlugin(repositories, pluginsDirectory)

  log.info("Initialization of scsSecurityPlugin completed")
}

executions {
  scsSecurityReload(httpMethod: "POST") { params ->
    initialize()
  }
}

download {
  beforeDownload { Request request, RepoPath repoPath ->
    scsPlugin.handleBeforeDownloadEvent(repoPath,request)
  }
}

storage {
  afterPropertyCreate { ItemInfo itemInfo, String propertyName, String[] propertyValues ->
    scsPlugin.handleAfterPropertyCreateEvent(security.currentUser(), itemInfo, propertyName, propertyValues)
  }
}
