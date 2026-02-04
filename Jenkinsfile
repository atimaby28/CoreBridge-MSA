pipeline {
    agent {
        kubernetes {
            yaml """
apiVersion: v1
kind: Pod
metadata:
    namespace: jenkins
spec:
    restartPolicy: Never
    containers:
        - name: gradle
          image: gradle:8.9-jdk21
          command: ['cat']
          tty: true
          volumeMounts:
              - name: workspace
                mountPath: /workspace

        - name: kaniko
          image: gcr.io/kaniko-project/executor:debug
          command: ['/busybox/sh', '-c', 'sleep infinity']
          tty: true
          volumeMounts:
              - name: docker-config
                mountPath: /kaniko/.docker
              - name: workspace
                mountPath: /workspace

        - name: kubectl
          image: bitnami/kubectl:latest
          command: ['cat']
          tty: true

    volumes:
        - name: workspace
          emptyDir: {}
        - name: docker-config
          secret:
              secretName: regcred
              items:
                  - key: .dockerconfigjson
                    path: config.json
"""
        }
    }

    environment {
        DOCKER_REGISTRY = 'atimaby12'
        NAMESPACE = 'corebridge'
        GIT_URL = 'https://github.com/atimaby28/CoreBridge-MSA.git'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
    }

    parameters {
        choice(
            name: 'SERVICE_NAME',
            choices: [
                'gateway',
                'user',
                'jobposting',
                'jobposting-comment',
                'jobposting-view',
                'jobposting-like',
                'jobposting-hot',
                'jobposting-read',
                'resume',
                'apply',
                'schedule',
                'notification',
                'admin-audit'
            ],
            description: '배포할 서비스 선택'
        )
    }

    stages {

        stage('Checkout') {
            steps {
                container('gradle') {
                    checkout([$class: 'GitSCM',
                        branches: [[name: '*/feature/deploy']],
                        userRemoteConfigs: [[url: "${GIT_URL}"]]
                    ])
                }
            }
        }

        stage('Build JAR') {
            steps {
                container('gradle') {
                    dir('backend') {
                        sh """
                            chmod +x ./gradlew
                            ./gradlew :service:${params.SERVICE_NAME}:bootJar -x test --no-daemon
                        """
                    }
                }
            }
        }

        stage('Build & Push Image') {
            steps {
                container('kaniko') {
                    sh """
                        /kaniko/executor \
                            --context=dir:///workspace/backend \
                            --dockerfile=/workspace/backend/Dockerfile \
                            --build-arg=SERVICE_NAME=${params.SERVICE_NAME} \
                            --destination=${DOCKER_REGISTRY}/corebridge-${params.SERVICE_NAME}:${IMAGE_TAG} \
                            --destination=${DOCKER_REGISTRY}/corebridge-${params.SERVICE_NAME}:latest
                    """
                }
            }
        }

        stage('Deploy to K3s') {
            steps {
                container('kubectl') {
                    sh """
                        kubectl set image deployment/corebridge-${params.SERVICE_NAME} \
                            ${params.SERVICE_NAME}=${DOCKER_REGISTRY}/corebridge-${params.SERVICE_NAME}:${IMAGE_TAG} \
                            -n ${NAMESPACE} || \
                        echo "Deployment not found - apply yaml first"
                    """
                }
            }
        }
    }

    post {
        success {
            echo "✅ ${params.SERVICE_NAME} 서비스 배포 성공 (Build #${IMAGE_TAG})"
        }
        failure {
            echo "❌ ${params.SERVICE_NAME} 서비스 배포 실패 (Build #${IMAGE_TAG})"
        }
    }
}
