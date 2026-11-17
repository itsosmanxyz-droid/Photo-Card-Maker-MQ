TERMUX দিয়ে বিল্ড (এডসসহ)
1. Termux (F-Droid থেকে) খুলে: termux-setup-storage
2. এই zip আনজিপ করে ফোল্ডারে যান: cd ~/storage/downloads/PCM_Gradle
3. bash termux_setup.sh      (একবারই, বড় ডাউনলোড)
4. ./build.sh                (APK: app/build/outputs/apk/debug/)
APK ইনস্টল: ফাইল ম্যানেজার থেকে খুলুন।
সমস্যা: aapt2 / build-tools এর x86 টুল Termux-এ চলে না; তাই aapt2 override দেওয়া আছে (gradle.properties)।
AdConfig.TEST_MODE এখন true — রিলিজের আগে false করবেন।

GITHUB ACTIONS (ফোনের ব্রাউজার থেকে):
1. github.com এ নতুন repository বানান।
2. এই ফোল্ডারের সব ফাইল (লুকানো .github ফোল্ডারসহ) আপলোড করুন।
3. Actions ট্যাব -> Build APK রান শেষ হলে নিচে Artifacts থেকে APK নামান।
