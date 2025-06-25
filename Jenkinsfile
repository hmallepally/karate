pipeline {
    agent any
    
    triggers {
        cron('TZ=America/Chicago\n0 5,17 * * *')
    }
    
    tools {
        jdk '17'
    }
    
    environment {
        GRADLE_OPTS = '-Dorg.gradle.daemon=false'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                script {
                    if (isUnix()) {
                        sh './gradlew clean build'
                    } else {
                        bat 'gradlew.bat clean build'
                    }
                }
            }
        }
        
        stage('Start Mock Server') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'nohup ./gradlew bootRun > mock-server.log 2>&1 &'
                        sh 'sleep 30'
                    } else {
                        bat 'start /B gradlew.bat bootRun'
                        bat 'timeout /t 30'
                    }
                }
            }
        }
        
        stage('Test') {
            steps {
                script {
                    if (isUnix()) {
                        sh './gradlew test'
                    } else {
                        bat 'gradlew.bat test'
                    }
                }
            }
            post {
                always {
                    publishTestResults testResultsPattern: 'build/test-results/test/*.xml'
                    archiveArtifacts artifacts: 'build/reports/**/*', allowEmptyArchive: true
                }
            }
        }
    }
    
    post {
        always {
            script {
                if (isUnix()) {
                    sh 'pkill -f "bootRun" || true'
                } else {
                    bat 'taskkill /F /IM java.exe /T || exit 0'
                }
            }
            cleanWs()
        }
        failure {
            emailext (
                subject: "Jenkins Build Failed: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                body: "Build failed. Check console output at ${env.BUILD_URL}",
                to: "${env.CHANGE_AUTHOR_EMAIL}"
            )
        }
    }
}
