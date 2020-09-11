#!/bin/bash
set -x
BUCKET_NAME=cf-templates-681v3yr6sl6i-us-west-2
BUCKET_URL="https://${BUCKET_NAME}.s3-us-west-2.amazonaws.com"
MASTER_ACCOUNT=956254777756
MASTER_PROFILE=PGE-Master-Dev
TARGET_ORG=ou-nfqe-bfrwogtk
REGIONS=us-west-2
for i in ec2
do
  TEMPLATE="${i}-config.yml"
  # TEMPLATE_URL="file://${FOLDER}/${TEMPLATE}" # Can't use 4 local files > 5120
  TEMPLATE_URL="${BUCKET_URL}/${TEMPLATE}"
  SSNAME="cscoe-config-"${i}"-stackset"
  FOLDER="aws-${i}"
  cd $FOLDER
  for j in *py
  do
  	BASE_NAME=`echo "${j%.py}"`
  	ZIP_FILE=${BASE_NAME}.zip
    echo "PPPPPPPACKAGING ${BASE_NAME}"
    zip ${ZIP_FILE} ${BASE_NAME}.py
    aws s3 cp ./${ZIP_FILE} s3://${BUCKET_NAME}/${ZIP_FILE} \
      --profile "${MASTER_PROFILE}"
    aws s3 cp ./${TEMPLATE} s3://${BUCKET_NAME}/${TEMPLATE} \
      --profile "${MASTER_PROFILE}"
    echo "DDDDDDEPLOYING $SSNAME"
    aws cloudformation create-stack --region us-west-2 \
    --template-url "https://cf-templates-681v3yr6sl6i-us-west-2.s3-us-west-2.amazonaws.com/test-config-ec2-stackset.yml" \
    --stack-name "${SSNAME}" \
    --capabilities CAPABILITY_IAM \
    --profile "${MASTER_PROFILE}"
    # --parameters file://../cscoe-config-rule-parameters.json \
  done
  cd ..
  exit 0
	echo "DDDDDDEPLOYING $SSNAME"
  aws cloudformation create-stack-set \
    --stack-set-name "${SSNAME}" \
    --template-url "${TEMPLATE_URL}" \
    --permission-model SERVICE_MANAGED --auto-deployment Enabled=true,RetainStacksOnAccountRemoval=false \
    --capabilities CAPABILITY_IAM \
    --parameters file://cscoe-config-rule-parameters.json \
    --profile "${MASTER_PROFILE}"
  aws cloudformation create-stack-instances \
    --stack-set-name "${SSNAME}" \
    --deployment-targets \
    OrganizationalUnitIds="${TARGET_ORG}" \
    --regions "${REGIONS}" \
    --operation-preferences FailureToleranceCount=0,MaxConcurrentCount=1 \
    --profile "${MASTER_PROFILE}"
# aws cloudformation delete-stack-instances \
  # --stack-set-name "${SSNAME}" \
  # --deployment-targets \
  #   OrganizationalUnitIds="${TARGET_ORG}" \
  # --regions "${REGIONS}" \
  # --no-retain-stacks \
  # --profile "${MASTER_PROFILE}"

  # aws cloudformation delete-stack-set\
    # --stack-set-name "${SSNAME}" \
    # --profile "${MASTER_PROFILE}"
done
exit 0
