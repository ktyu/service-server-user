#!/usr/bin/env bash
set -euo pipefail

########################################
# 설정값
########################################
KEY_FILE="./develop.pem"          # 현재 폴더에 있는 pem 키
REMOTE_USER="ec2-user"              # 서버 계정
REMOTE_HOST="dev-bastion.service.co.kr"     # 서버 주소 또는 IP
REMOTE_DIR="/home/ec2-user"     # JAR 복사할 서버 디렉토리
RUN_SCRIPT="deploy_run.sh"               # 서버에서 실행할 스크립트 이름 (REMOTE_DIR 안에 있다고 가정)

########################################
# 1. Gradle clean build (JAR 빌드)
########################################
echo "[1/3] Gradle clean build 시작..."

if [[ -x "./gradlew" ]]; then
  # 로컬에 gradlew 있으면 wrapper 사용
  ./gradlew clean build
else
  # 없으면 시스템 gradle 사용
  gradle clean build
fi

echo "[1/3] Gradle 빌드 완료."

########################################
# 2. JAR 파일에 timestamp 붙여서 이름 만들기
########################################
echo "[2/3] JAR 파일 찾는 중..."

# 가장 최근에 생성된 JAR 하나 선택 (build/libs 기준)
ORIGINAL_JAR=$(ls -t build/libs/*.jar | grep -v 'plain' | head -n 1)

if [[ -z "${ORIGINAL_JAR}" ]]; then
  echo "ERROR: build/libs 안에서 JAR 파일을 찾을 수 없습니다."
  exit 1
fi

echo "빌드된 JAR: ${ORIGINAL_JAR}"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BASENAME=$(basename "${ORIGINAL_JAR}" .jar)
NEW_JAR="${BASENAME}-${TIMESTAMP}.jar"

########################################
# 3. scp로 서버에 복사 후, run.sh 실행
########################################
echo "[3/3] SCP로 서버에 JAR 전송..."

chmod 600 "${KEY_FILE}"

# 📦 scp로 보낼 때 이름을 변경하여 전송
scp -i "${KEY_FILE}" "${ORIGINAL_JAR}" "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}/${NEW_JAR}"

echo "JAR 전송 완료. 서버에서 ${RUN_SCRIPT} 실행..."

ssh -i "${KEY_FILE}" "${REMOTE_USER}@${REMOTE_HOST}" \
  "${REMOTE_DIR}/${RUN_SCRIPT} ${NEW_JAR}"

echo "배포 및 실행 완료 ✅"

