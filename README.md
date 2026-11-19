# Photo Card Maker MQ

Termux থেকে (একবার):
```
pkg install git unzip
unzip PhotoCardMakerMQ.zip -d PhotoCardMakerMQ && cd PhotoCardMakerMQ
git init -b main && git add . && git commit -m "Initial project"
git remote add origin https://github.com/YOUR_USER/YOUR_REPO.git
git push -u origin main
```
তারপর GitHub > Actions > Build > Artifacts > Photo-Card-Maker-MQ-APK।

বদলাতে হবে: app/build.gradle (REMOTE_TEMPLATES_URL, PRIVACY_URL, SUPPORT_EMAIL), gradle.properties (AdMob release ID)।
Remote templates: আলাদা repo-তে templates.json রাখুন (ফরম্যাট = app/src/main/assets/templates.json)।
