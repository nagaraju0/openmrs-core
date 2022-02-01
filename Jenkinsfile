pipeline {
    agent { label 'jdk11-mvn3.8.4' }
    triggers {
        upstream(upstreamProjects: 'shopizer', threshold: hudson.model.Result.SUCCESS)
        cron('15 */1 * * 1-5')
        pollSCM('30 23 * * 1-5') 
    }
    parameters {
        choice(name: 'BRANCH_TO_BUILD', choices: ['check', 'jan13', 'master'], description: 'forbuild')
        string(name: 'MAVEN_GOAL', defaultValue: 'clean package', description: 'forgoal')
    }
    tools {
        maven 'MVN_3.8.4'
    }
    stages {
        stage('Get Clone Code') {
            steps {
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
                // archiving artifactory
                archive "target/*.jar"
            }
        }
        stage('Publish Test Result') {
            steps {
                junit '**/TEST-*.xml'
            }
        }
        stage('build & SonarQube analysis') {
            steps {
                withSonarQubeEnv(installationName:'SONAR_9.2.1') {
                    sh 'mvn clean install sonar:sonar'
                }
            }
        }
        stage("Quality Gate") {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
    }
}