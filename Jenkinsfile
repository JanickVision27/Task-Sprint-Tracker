/*
 * Jenkins Declarative Pipeline for the Spring Boot backend.
 * The Jenkins agent must be Linux/Unix with Docker access and Trivy installed.
 */
pipeline {
    // Use any configured agent; this pipeline expects the selected node to provide the tools below.
    agent any

    // These names must match installations configured in Manage Jenkins > Tools.
    tools {
        jdk 'JDK21'
        maven 'Maven3'
    }

    environment {
        // Keep the local image name stable and use the Jenkins build number for traceable tags.
        IMAGE_NAME = 'task-sprint-tracker-backend'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
    }

    options {
        // Disable Jenkins' implicit checkout so the explicit Checkout stage is the only checkout.
        skipDefaultCheckout(true)
    }

    stages {
        stage('Checkout') {
            steps {
                // Check out the exact SCM revision that triggered this pipeline run.
                checkout scm
            }
        }

        stage('Build') {
            steps {
                // Compile production sources first; tests are kept in their own reportable stage.
                dir('Backend') {
                    sh 'mvn -B clean compile'
                }
            }
        }

        stage('Test') {
            steps {
                // Run JUnit tests from the Maven project's root directory.
                dir('Backend') {
                    sh 'mvn -B test'
                }
            }
            post {
                always {
                    // Publish reports even when tests fail so Jenkins shows test-level results.
                    junit testResults: 'Backend/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                // Produce the executable JAR; tests already ran in the Test stage.
                dir('Backend') {
                    sh 'mvn -B package -DskipTests'
                }
            }
        }

        stage('Docker Build') {
            steps {
                // Backend is the build context so Backend/.dockerignore and Backend/Dockerfile apply.
                // This builds locally on the Jenkins agent; it does not push to a registry.
                sh 'docker build --tag "$IMAGE_NAME:$IMAGE_TAG" Backend'
            }
        }

        stage('Trivy Image Scan') {
            steps {
                // Report high/critical vulnerabilities without failing the build during initial adoption.
                // Trivy must be installed on the selected Jenkins agent.
                sh 'trivy image --exit-code 0 --severity HIGH,CRITICAL "$IMAGE_NAME:$IMAGE_TAG"'
            }
        }
    }

    post {
        always {
            // Delete checked-out source and generated artifacts on both success and failure.
            deleteDir()
        }
    }
}
