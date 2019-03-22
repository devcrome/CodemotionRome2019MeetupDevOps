pipeline {
    agent { label "master" }
    stages {
        stage('Configuration') {
            steps {
                script {
                    echo sh(script: 'env|sort', returnStdout: true)
                    gitProjectUrl = "${pipe_project_url}"
                    gitProjectBranch = "${pipe_project_branch}"
                    namespace = "${pipe_project_branch}"

                    slaveAbsolutePath = pwd()
                    kubeConfigPath = "${slaveAbsolutePath}"
                    k8sFolder = 'k8s'

                    currentBuild.displayName = "${env.BUILD_DISPLAY_NAME}-${GIT_COMMIT_SHORT}"
                }
            }
        }
        stage('Git checkout') {
            steps {
                script {

                    checkout([$class                           : 'GitSCM',
                              branches                         : [[name: "${gitProjectBranch}"]],
                              doGenerateSubmoduleConfigurations: false,
                              extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                                   relativeTargetDir: "${k8sFolder}"],
                                                                  [$class: 'CloneOption', shallow: true]],
                              submoduleCfg                     : [],
                              userRemoteConfigs                : [[
                                                                    // credentialsId: "${gitCredentialsId}",
                                                                    url          : "${gitProjectUrl}"
                                                                   ]]
                    ])

                }
            }
        }
        stage('Load git infos') {
            dir("${k8sFolder}") {
                steps {
                    script {
                        GIT_COMMIT_SHORT = sh(
                            script: "printf \$(git rev-parse --short ${GIT_COMMIT})",
                            returnStdout: true
                        )
                        echo "${GIT_COMMIT_SHORT}"
                        currentBuild.displayName = "${env.BUILD_DISPLAY_NAME}-${GIT_COMMIT_SHORT}"
                    }
                }
            }
        }
        stage('Deploy on kubernetes') {
            dir("${k8sFolder}") {
                steps {
                    script {
                        sh "kubectl apply -f . "
                    }
                }
            }
        }
        stage('Rollout status') {
            dir("${k8sFolder}") {
                steps {
                    script {
                        sh "kubectl rollout status deployment/codemotion-dev-circle-front --namespace ${namespace}"
                    }
                }
            }
        }
        stage('Watch service status') {
            dir("${k8sFolder}") {
                steps {
                    script {
                        sh "kubectl get service codemotion-dev-circle-front --namespace ${namespace}"
                    }
                }
            }
        }
    }
}

