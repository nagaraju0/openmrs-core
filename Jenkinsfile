pipeline {
    agent { label 'jdk11-mvn3.8.4' }
    triggers {
        upstream(upstreamProjects: 'spc', threshold: hudson.model.Result.SUCCESS)
        cron('30 */2 * * 0,6')
        pollSCM('45 23 * * 0,6')
    }
    parameters {
        choice(name: 'BRANCH_BUILD', choices: ['jan13', 'master'], description: 'To branch')
        string(name: 'MAVAEN_RUN', defaultValue: 'clean package', description: 'For run')
    }
    tools {
        maven 'MVN_3.8.4'
    }
    stages {
        stage('Get Git Clone') {
            steps{
                git 'https://github.com/nagaraju0/openmrs-core.git' 
            }
        }
        stage('To Build') {
            steps {
                sh 'mvn clean package'
            }
        }
        stage('Archive') {
            steps {
                // archive artifacts
                archive 'target/*.jar'
            }
        }
        stage('Publish Test Results') {
            steps {
                junit '**TEST-*.xml'
            }
        }
        stage('build & SonarQube analysis') {
            steps {
                withSonarQubeEnv(installationName:'SONAR_9.2.1') {
                    sh 'mvn clean package sonar:sonar'
                }
            }
        }
        stage('Quality Gate') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        stage ('Artifactory configuration') {
            steps {
                rtMavenDeployer (
                    id: "MAVEN_DEPLOYER",
                    serverId: "JFROG-OSS",
                    releaseRepo: "qt-maven-rleases",
                    snapshotRepo: "qt-maven-snapshots"
                )
            }
        }
        stage ('Exec Maven') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'JFROG_ARTIFACTROY', usernameVariable: 'ARTIFACTORY_USERNAME', passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
                    rtMavenRun (
                        tool: 'MVN_3.8.4',
                        pom: 'pom.xml',
                        goals: 'clean install',
                        deployerId: "MAVEN_DEPLOYER"
                    )
                }
            }
        }
        stage ('Publish build info') {
            steps {
                PublishBuildInfo (
                    serverId: "JFROG-OSS"
                )
            }
        }
    }
    post {
        always {
            mail from: "abbaigarimanavadu77@gmail.com",
                to: "nagarajucivil011@gmail.com",
                subject: "status of the pipeline : ${currntBuild.fullDisplayName}",
                body: "${env.BUILD_URL} has the result ${currentBuild.result}"
        }
    }
         
}
