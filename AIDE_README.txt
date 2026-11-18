AIDE-তে এডসসহ (পুরনো SDK 22.6.0 + UMP 2.2.0)
1. zip আনজিপ করুন, AIDE-তে এই ফোল্ডারটা Gradle প্রজেক্ট হিসেবে খুলুন (বর্তমান ফোল্ডার = যেখানে settings.gradle আছে)।
2. প্রথমবার AIDE নিজে dependency ডাউনলোড করবে (ইন্টারনেট লাগবে)।
3. কাজ না করলে এডস ছাড়া সংস্করণ: ads/AdsManager.java কে PhotoCardMakerMQ_final_v5_noads.zip এর ads_stub/AdsManager.java দিয়ে বদলান।
AdConfig.TEST_MODE = true (টেস্ট এড)। রিলিজে false।
