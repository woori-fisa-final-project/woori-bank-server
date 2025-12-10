pipeline {
    agent any

    environment {
        AWS_HOST     = "43.201.222.157"
        DOCKER_IMAGE = "bae1234/woori-bank-server:latest"
    }

    stages {

        /* --------------------------
         * 1) Git Checkout
         * -------------------------- */
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        /* --------------------------
         * 2) Gradle Build
         * -------------------------- */
        stage('Gradle Build') {
            steps {
                sh '''
                chmod +x gradlew
                ./gradlew clean build -x test
                '''
            }
        }

        /* --------------------------
         * 3) Docker Build
         * -------------------------- */
        stage('Docker Build') {
            steps {
                sh "docker rmi -f ${DOCKER_IMAGE} || true"
                sh "docker build -t ${DOCKER_IMAGE} ."
            }
        }

        /* --------------------------
         * 4) Docker Hub Push
         * -------------------------- */
        stage('Docker Push') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-cred',
                        usernameVariable: 'DOCKERHUB_USR',
                        passwordVariable: 'DOCKERHUB_PSW'
                    )
                ]) {
                    sh '''
                    echo "$DOCKERHUB_PSW" | docker login -u "$DOCKERHUB_USR" --password-stdin
                    docker push ${DOCKER_IMAGE}
                    '''
                }
            }
        }

        /* --------------------------
         * 5) Deploy to AWS EC2
         * -------------------------- */
        stage('Deploy to AWS') {
            steps {
                sshagent(['aws-ssh-key']) {

                    withCredentials([
                        usernamePassword(
                            credentialsId: 'db-credential',
                            usernameVariable: 'DB_USER',
                            passwordVariable: 'DB_PASS'
                        ),
                        string(credentialsId: 'bank-db-url', variable: 'BANK_DB_URL'),
                        string(credentialsId: 'jwt-secret', variable: 'JWT_SECRET')
                    ]) {

                        sh '''
                        ssh -o StrictHostKeyChecking=no ubuntu@${AWS_HOST} << EOF
# 최신 이미지 pull
docker pull ${DOCKER_IMAGE}

# 기존 컨테이너 정리
docker rm -f woori_bank_server || true

# 새 컨테이너 실행
docker run -d --name woori_bank_server -p 8081:8081 \
  -e SPRING_DATASOURCE_URL="${BANK_DB_URL}" \
  -e SPRING_DATASOURCE_USERNAME="${DB_USER}" \
  -e SPRING_DATASOURCE_PASSWORD="${DB_PASS}" \
  -e JWT_SECRET="${JWT_SECRET}" \
  -e SPRING_DATA_REDIS_HOST="172.31.12.253" \
  -e SPRING_DATA_REDIS_PORT="6379" \
  -e SPRING_DATA_REDIS_DATABASE="0" \
  ${DOCKER_IMAGE}

# 불필요한 이미지 정리
docker image prune -f

EOF
                        '''
                    }
                }
            }
        }
    }
}

