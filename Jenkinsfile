pipeline {
    agent any
    tools {
        jdk 'JDK-17'
        maven 'Maven'
    }
    options {
        timestamps()
        buildDiscarder(logRotator(
            numToKeepStr: '20',
            artifactNumToKeepStr: '10'
        ))
        timeout(time: 40, unit: 'MINUTES')
    }
    environment {
        NAMESPACE = "polyglot-app"
        DOCKER_USER = "vishnumohan9447"
        IMAGE_TAG = "${BUILD_NUMBER}"
        SONARQUBE_ENV = "SonarQube"
    }
    stages {
        stage('Checkout') {
            steps {
                git credentialsId: 'github-token',
                    url: 'https://github.com/vishnu-mohan-9447/multi-language-app.git',
                    branch: 'main'
            }
        }
        stage('Build & Unit Test') {
            steps {
                dir('java-module') {
                    sh 'mvn clean verify'
                }
            }
        }
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv("${SONARQUBE_ENV}") {
                    withCredentials([
                        string(
                            credentialsId: 'sonar-token',
                            variable: 'SONAR_TOKEN'
                        )
                    ]) {
                        dir('java-module') {
                            sh """
                                mvn sonar:sonar \
                                -Dsonar.projectKey=polyglot-app \
                                -Dsonar.projectName=polyglot-app \
                                -Dsonar.token=\$SONAR_TOKEN
                            """
                        }
                    }
                }
            }
        }
        stage('Quality Gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        stage('Trivy Filesystem Scan') {
            steps {
                sh '''
                    trivy fs . \
                    --severity HIGH,CRITICAL \
                    --exit-code 0
                '''
            }
        }
        stage('Build Docker Images') {
            steps {
                sh """
                    docker build -t ${DOCKER_USER}/poly-glot-go-module:${IMAGE_TAG} ./go-module
                    docker build -t ${DOCKER_USER}/poly-glot-nodejs-module:${IMAGE_TAG} ./nodejs-module
                    docker build -t ${DOCKER_USER}/poly-glot-python-module:${IMAGE_TAG} ./python-module
                    docker build -t ${DOCKER_USER}/poly-glot-java-frontend:${IMAGE_TAG} ./java-module
                """
            }
        }
        stage('Trivy Image Scan') {
            steps {
                sh """
                    trivy image ${DOCKER_USER}/poly-glot-go-module:${IMAGE_TAG} --severity HIGH,CRITICAL --exit-code 0
                    trivy image ${DOCKER_USER}/poly-glot-nodejs-module:${IMAGE_TAG} --severity HIGH,CRITICAL --exit-code 0
                    trivy image ${DOCKER_USER}/poly-glot-python-module:${IMAGE_TAG} --severity HIGH,CRITICAL --exit-code 0
                    trivy image ${DOCKER_USER}/poly-glot-java-frontend:${IMAGE_TAG} --severity HIGH,CRITICAL --exit-code 0
                """
            }
        }
        stage('Push Images') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-token',
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )
                ]) {
                    sh """
                        echo \$DOCKER_PASSWORD | docker login -u \$DOCKER_USERNAME --password-stdin
                        docker push ${DOCKER_USER}/poly-glot-go-module:${IMAGE_TAG}
                        docker push ${DOCKER_USER}/poly-glot-nodejs-module:${IMAGE_TAG}
                        docker push ${DOCKER_USER}/poly-glot-python-module:${IMAGE_TAG}
                        docker push ${DOCKER_USER}/poly-glot-java-frontend:${IMAGE_TAG}
                    """
                }
            }
        }
        stage('Create Namespace') {
            steps {
                sh """
                    kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                """
            }
        }
        stage('Deploy to Kubernetes + Istio') {
            steps {
                script {
                    echo "Applying Kubernetes base manifests..."
                    sh """
                        kubectl apply -k k8s/base -n ${NAMESPACE} || kubectl apply -f k8s/base/ -n ${NAMESPACE}
                    """

                    echo "Applying Istio resources (Gateway, VirtualService, DestinationRule)..."
                    sh """
                        kubectl apply -f k8s/istio/ -n ${NAMESPACE}
                    """

                    echo "Updating container images..."
                    sh """
                        kubectl set image deployment/go-module go-module=${DOCKER_USER}/poly-glot-go-module:${IMAGE_TAG} -n ${NAMESPACE}
                        kubectl set image deployment/nodejs-module nodejs-module=${DOCKER_USER}/poly-glot-nodejs-module:${IMAGE_TAG} -n ${NAMESPACE}
                        kubectl set image deployment/python-module python-module=${DOCKER_USER}/poly-glot-python-module:${IMAGE_TAG} -n ${NAMESPACE}
                        kubectl set image deployment/java-frontend java-frontend=${DOCKER_USER}/poly-glot-java-frontend:${IMAGE_TAG} -n ${NAMESPACE}
                    """

                    echo "Restarting deployments to ensure Istio sidecars are injected..."
                    sh "kubectl rollout restart deployment -n ${NAMESPACE}"
                }
            }
        }
        stage('Verify Rollout') {
            steps {
                sh """
                    echo "Waiting for deployments to stabilize..."
                    kubectl rollout status deployment/go-module -n ${NAMESPACE} --timeout=300s
                    kubectl rollout status deployment/nodejs-module -n ${NAMESPACE} --timeout=180s
                    kubectl rollout status deployment/python-module -n ${NAMESPACE} --timeout=180s
                    kubectl rollout status deployment/java-frontend -n ${NAMESPACE} --timeout=300s
                """
            }
        }
    }
    post {
        success {
            echo 'CI/CD Pipeline with Istio Traffic Splitting Completed Successfully!'
            echo 'Access your app via Istio Ingress Gateway'
        }
        failure {
            sh """
                echo "Deployment Failed. Current status:"
                kubectl get pods,svc,deploy -n ${NAMESPACE} || true
                kubectl get gateway,virtualservice,destinationrule -n ${NAMESPACE} || true
            """
        }
        always {
            cleanWs()
        }
    }
}
