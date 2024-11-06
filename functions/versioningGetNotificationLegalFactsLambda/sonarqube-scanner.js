let options = {
  "sonar.organization": "pagopa",
  "sonar.projectKey": "pagopa_versioningGetNotificationLegalFactsLambda",
};

if (process.env.PR_NUM) {
  options["sonar.pullrequest.base"] = process.env.BRANCH_TARGET;
  options["sonar.pullrequest.branch"] = process.env.BRANCH_NAME;
  options["sonar.pullrequest.key"] = process.env.PR_NUM;
}

const scanner = require("sonarqube-scanner");

scanner(
  {
    serverUrl: "https://sonarcloud.io",
    token: process.env.SONAR_TOKEN,
    options: options,
  },
  () => process.exit()
);
