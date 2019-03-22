pipeline {
    agent { label "master" }
    stages {
        stage('Configuration') {
            steps {
                script {
                    echo sh(script: 'env|sort', returnStdout: true)
                    GIT_COMMIT_SHORT = sh(
                        script: "printf \$(git rev-parse --short ${GIT_COMMIT})",
                        returnStdout: true
                    )
                    echo "${GIT_COMMIT_SHORT}"

                    gitProjectUrl = "${pipe_project_url}"
                    gitProjectBranch = "${pipe_project_branch}"
                    namespace = "${pipe_project_branch}"

                    slaveAbsolutePath = pwd()
                    kubeConfigPath = "${slaveAbsolutePath}"

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
                                                                   relativeTargetDir: "."],
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
        stage('Deploy on kubernetes') {
            steps {
                script {
                    sh "kubectl apply -f . "
                }
            }
        }
        stage('Rollout status') {
            steps {
                script {
                    sh "kubectl rollout status deployment/codemotion-dev-circle-front --namespace ${namespace}"
                }
            }
        }
        stage('Watch service status') {
            steps {
                script {
                    sh "kubectl get service codemotion-dev-circle-front --namespace ${namespace}"
                }
            }
        }
    }
}

