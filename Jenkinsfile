pipeline {

```
agent any

tools {
    jdk 'JDK-17'
    maven 'Maven-3.9'
}

options {
    timestamps()
    buildDiscarder(logRotator(
        numToKeepStr: '20',
        artifactNumToKeepStr: '10'
    ))
    timeout(time: 30, unit: 'MINUTES')
}

environment {
    NAMESPACE = "polyglot-app"
    DOCKER_USER = "YOUR_DOCKERHUB_USERNAME"
    IMAGE_TAG = "${BUILD_NUMBER}"
    SONARQUBE_ENV = "SonarQube"
}

stages {

    stage('Checkout') {
        steps {
            checkout scm
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

                        sh '''
                            mvn sonar:sonar \
                            -Dsonar.projectKey=polyglot-app \
                            -Dsonar.projectName=polyglot-app \
                            -Dsonar.token=$SONAR_TOKEN
                        '''
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
                --exit-code 1
            '''
        }
    }

    stage('Build Docker Images') {
        steps {

            sh """
                docker build -t ${DOCKER_USER}/go-module:${IMAGE_TAG} ./go-module

                docker build -t ${DOCKER_USER}/nodejs-module:${IMAGE_TAG} ./nodejs-module

                docker build -t ${DOCKER_USER}/python-module:${IMAGE_TAG} ./python-module

                docker build -t ${DOCKER_USER}/java-module:${IMAGE_TAG} ./java-module
            """
        }
    }

    stage('Trivy Image Scan') {
        steps {

            sh """
                trivy image ${DOCKER_USER}/go-module:${IMAGE_TAG} \
                --severity HIGH,CRITICAL \
                --exit-code 1

                trivy image ${DOCKER_USER}/nodejs-module:${IMAGE_TAG} \
                --severity HIGH,CRITICAL \
                --exit-code 1

                trivy image ${DOCKER_USER}/python-module:${IMAGE_TAG} \
                --severity HIGH,CRITICAL \
                --exit-code 1

                trivy image ${DOCKER_USER}/java-module:${IMAGE_TAG} \
                --severity HIGH,CRITICAL \
                --exit-code 1
            """
        }
    }

    stage('Push Images') {

        steps {

            withCredentials([
                usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKER_USERNAME',
                    passwordVariable: 'DOCKER_PASSWORD'
                )
            ]) {

                sh '''
                    echo $DOCKER_PASSWORD | docker login \
                    -u $DOCKER_USERNAME \
                    --password-stdin

                    docker push $DOCKER_USER/go-module:$IMAGE_TAG

                    docker push $DOCKER_USER/nodejs-module:$IMAGE_TAG

                    docker push $DOCKER_USER/python-module:$IMAGE_TAG

                    docker push $DOCKER_USER/java-module:$IMAGE_TAG
                '''
            }
        }
    }

    stage('Create Namespace') {
        steps {

            sh '''
                kubectl create namespace ${NAMESPACE} \
                --dry-run=client -o yaml | kubectl apply -f -
            '''
        }
    }

    stage('Deploy to Kubernetes') {

        steps {

            sh """

                kubectl set image deployment/go-module \
                go-module=${DOCKER_USER}/go-module:${IMAGE_TAG} \
                -n ${NAMESPACE}

                kubectl set image deployment/nodejs-module \
                nodejs-module=${DOCKER_USER}/nodejs-module:${IMAGE_TAG} \
                -n ${NAMESPACE}

                kubectl set image deployment/python-module \
                python-module=${DOCKER_USER}/python-module:${IMAGE_TAG} \
                -n ${NAMESPACE}

                kubectl set image deployment/java-frontend \
                java-frontend=${DOCKER_USER}/java-module:${IMAGE_TAG} \
                -n ${NAMESPACE}
            """
        }
    }

    stage('Verify Rollout') {

        steps {

            sh '''
                kubectl rollout status deployment/go-module \
                -n polyglot-app --timeout=300s

                kubectl rollout status deployment/nodejs-module \
                -n polyglot-app --timeout=300s

                kubectl rollout status deployment/python-module \
                -n polyglot-app --timeout=300s

                kubectl rollout status deployment/java-frontend \
                -n polyglot-app --timeout=300s
            '''
        }
    }
}

post {

    success {
        echo 'CI/CD Pipeline Completed Successfully'
    }

    failure {

        sh '''
            kubectl get pods -n polyglot-app || true
            kubectl get svc -n polyglot-app || true
            kubectl get deployments -n polyglot-app || true
        '''
    }

    always {
        cleanWs()
    }
}
```

}

