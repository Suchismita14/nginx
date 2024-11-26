def call(Map pipelineParams = [:]) {
    def config = readYaml(file: libraryResource('config/nginx-config.yml'))

    pipeline {
        agent any
        stages {
            stage('Clone Repository') {
                steps {
                    script {
                        echo "Cloning repository..."
                        checkout([$class: 'GitSCM',
                                  branches: [[name: config.branch]],
                                  userRemoteConfigs: [[url: config.repo_url]]])
                    }
                }
            }
            stage('User Approval') {
                steps {
                    input message: "Approve deployment of NGINX?", submitter: pipelineParams.submitter ?: 'admin'
                }
            }
            stage('Playbook Execution') {
                steps {
                    script {
                        echo "Executing Ansible Playbook..."
                        AnsibleUtils.runPlaybook(config.inventory_file, config.playbook)
                    }
                }
            }
            stage('Notification') {
                steps {
                    script {
                        echo "Sending notifications..."
                        NotificationUtils.sendSlackNotification(config.slack_webhook, "NGINX Deployment Successful!")
                        NotificationUtils.sendEmail(config.email_recipients, "NGINX Deployment Complete", "The deployment completed successfully.")
                    }
                }
            }
        }
    }
}
