pipeline {
    agent { label "master" }
    stages {
        stage('Configuration') {
            steps {
                script {
                    echo sh(script: 'env|sort', returnStdout: true)

                    gitProjectUrl = "${pipe_project_url}"
                    gitProjectBranch = "${pipe_project_branch}"

                    slaveAbsolutePath = pwd()
                    kubeConfigPath = "${slaveAbsolutePath}"
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
        stage('Watch service status') {
            steps {
                script {
                    sh "kubectl get service codemotion-dev-circle-front --namespace develop"
                }
            }
        }
        // stage('Force reload of Deployment') {
        //     steps {
        //             script {
        //                 echo "-> ${k8sAppName}"
        //                 def reloadPatch = '{\\"spec\\":{\\"template\\":{\\"metadata\\":{\\"labels\\":{\\"date\\":\\"`date +\'%s\'`\\"}}}}}'
        //                 sh "kubectl patch deployment ${k8sAppName} -p \"${reloadPatch}\" --kubeconfig ${kubeConfigPath}/kubeconf"
        //             }
        //     }
        // }
        // stage('Rollout status') {
        //     steps {
        //         script {
        //             sh "kubectl rollout status deployment/${k8sAppName} --kubeconfig ${kubeConfigPath}/kubeconf "
        //         }
        //     }
        // }
    }
}

