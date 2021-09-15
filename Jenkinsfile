@Library('jenkins-pipeline') _
import com.figure.Common

def common
pipeline {
    agent any

    options { timestamps() }

    tools{
        jdk 'JDK11'
    }
    stages {
        stage('Stage Checkout') {
            steps {
                script {
                    common = new Common(this)
                }
                gitCheckout()
            }
        }
        stage('Gradle Build') {
            steps {
                sh "./gradlew --no-daemon --parallel clean build  --refresh-dependencies -x test -x ktlint -PartifactVersion=${common.fixedBranchName()}-${env.BUILD_NUMBER}"
            }
        }
        stage('Deployment') {
            stages {
                stage('Docker Build') {
                    steps {
                        script {
                            dockerBuild("service", common.dockerTag(), "docker/Dockerfile")
                            if (env.BRANCH_NAME == env.CI_BRANCH) {
                                dockerTag(common.dockerTag, common.dockerLatestTag)
                            }
                        }
                    }
                }
                stage('Docker Push') {
                    steps {
                        script {
                            dockerPush(common.dockerTag())
                            if (env.BRANCH_NAME == env.CI_BRANCH) {
                                dockerPush(common.dockerLatestTag)
                            }
                        }
                    }
                }
            }
        }
    }
}

