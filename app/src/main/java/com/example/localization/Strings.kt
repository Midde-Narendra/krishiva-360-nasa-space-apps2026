package com.example.localization

object Strings {
    fun appTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "KRISHIVA 360"
        AppLanguage.TELUGU -> "కృషివా 360"
        AppLanguage.HINDI -> "कृषिवा 360"
    }

    fun tagline(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Soil Intelligence → Smart Decisions → Balanced Cropping → Sustainable Agriculture"
        AppLanguage.TELUGU -> "నేల విజ్ఞానం → తెలివైన నిర్ణయాలు → సమతుల్య సాగు → స్థిరమైన వ్యవసాయం"
        AppLanguage.HINDI -> "मृदा बुद्धिमत्ता → स्मार्ट निर्णय → संतुलित फसल चक्र → सतत कृषि"
    }

    fun coreStatement(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Before KRISHIVA recommends what to grow, it first understands what the soil can support."
        AppLanguage.TELUGU -> "ఏ పంట వేయాలో సిఫార్సు చేయడానికి ముందు, నేల దేనికి అనుకూలమో కృషివా అర్థం చేసుకుంటుంది."
        AppLanguage.HINDI -> "क्या उगाना है इसकी सिफारिश करने से पहले, कृषिवा पहले यह समझता है कि मिट्टी क्या संभाल सकती है।"
    }

    fun secondaryStatement(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "KRISHIVA 360 connects measured soil moisture with field planning."
        AppLanguage.TELUGU -> "కృషివా 360 కొలిచిన నేల తేమను సాగు ప్రణాళికతో అనుసంధానిస్తుంది."
        AppLanguage.HINDI -> "कृषिवा 360 मापी गई मिट्टी की नमी को खेत की योजना से जोड़ता है।"
    }

    fun welcomeHeadline(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Understand Your Soil. Plan Your Crop."
        AppLanguage.TELUGU -> "మీ నేలను అర్థం చేసుకోండి. మీ పంటను ప్రణాళిక చేయండి."
        AppLanguage.HINDI -> "अपनी मिट्टी को समझें। अपनी फसल की योजना बनाएं।"
    }

    fun welcomeSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Connect your ESP32 DevKit soil-moisture sensor over local 2.4 GHz Wi-Fi for live field measurements."
        AppLanguage.TELUGU -> "ప్రత్యక్ష నేల తేమ కొలతల కోసం స్థానిక 2.4 GHz Wi-Fi ద్వారా మీ ESP32 సెన్సార్‌ను కనెక్ట్ చేయండి."
        AppLanguage.HINDI -> "लाइव खेत माप के लिए स्थानीय 2.4 GHz वाई-फाई पर अपने ESP32 मिट्टी-नमी सेंसर को कनेक्ट करें।"
    }

    fun getStarted(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Set Up Farm Profile"
        AppLanguage.TELUGU -> "పొలం ప్రొఫైల్ సెటప్ చేయండి"
        AppLanguage.HINDI -> "खेत प्रोफ़ाइल सेट करें"
    }

    fun openDashboard(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Open Live Dashboard"
        AppLanguage.TELUGU -> "డాష్‌బోర్డ్ తెరవండి"
        AppLanguage.HINDI -> "लाइव डैशबोर्ड खोलें"
    }

    fun hardwareBadge(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "ESP32 DevKit • GPIO34 Analog Soil Moisture Telemetry"
        AppLanguage.TELUGU -> "ESP32 DevKit • GPIO34 నేల తేమ టెలిమెట్రీ"
        AppLanguage.HINDI -> "ESP32 DevKit • GPIO34 मृदा नमी टेलीमेट्री"
    }

    fun home(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Home"
        AppLanguage.TELUGU -> "హోమ్"
        AppLanguage.HINDI -> "होम"
    }

    fun scan(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Scan"
        AppLanguage.TELUGU -> "స్కాన్"
        AppLanguage.HINDI -> "स्कैन"
    }

    fun insights(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Insights"
        AppLanguage.TELUGU -> "విశ్లేషణ"
        AppLanguage.HINDI -> "इनसाइट्स"
    }

    fun demand(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Demand"
        AppLanguage.TELUGU -> "డిమాండ్"
        AppLanguage.HINDI -> "मांग"
    }

    fun profile(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Profile"
        AppLanguage.TELUGU -> "ప్రొఫైల్"
        AppLanguage.HINDI -> "प्रोफ़ाइल"
    }

    fun farmHealthOverview(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Live Soil Telemetry Overview"
        AppLanguage.TELUGU -> "ప్రత్యక్ష నేల టెలిమెట్రీ అవలోకనం"
        AppLanguage.HINDI -> "लाइव मृदा टेलीमेट्री अवलोकन"
    }

    fun quickActions(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "QUICK ACTIONS"
        AppLanguage.TELUGU -> "త్వరిత చర్యలు"
        AppLanguage.HINDI -> "त्वरित क्रियाएं"
    }

    fun todaysInsights(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "FIELD STATUS & SENSORS"
        AppLanguage.TELUGU -> "పొలం స్థితి & సెన్సార్లు"
        AppLanguage.HINDI -> "खेत की स्थिति और सेंसर"
    }

    fun soilMoisture(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Relative Moisture (%)"
        AppLanguage.TELUGU -> "సాపేక్ష తేమ (%)"
        AppLanguage.HINDI -> "सापेक्ष नमी (%)"
    }

    fun calibratedMoistureLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Calibrated soil-moisture estimate (%)"
        AppLanguage.TELUGU -> "కాలిబ్రేట్ చేసిన నేల తేమ అంచనా (%)"
        AppLanguage.HINDI -> "अंशांकित मिट्टी-नमी अनुमान (%)"
    }

    fun calibrationRequired(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Calibration required"
        AppLanguage.TELUGU -> "కాలిబ్రేషన్ అవసరం"
        AppLanguage.HINDI -> "कैलिब्रेशन आवश्यक है"
    }

    fun waitingForEsp32(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Waiting for ESP32"
        AppLanguage.TELUGU -> "ESP32 కోసం వేచి ఉంది"
        AppLanguage.HINDI -> "ESP32 की प्रतीक्षा है"
    }

    fun notConnected(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Not connected"
        AppLanguage.TELUGU -> "కనెక్ట్ చేయబడలేదు"
        AppLanguage.HINDI -> "कनेक्ट नहीं है"
    }

    fun notAvailable(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Not available"
        AppLanguage.TELUGU -> "అందుబాటులో లేదు"
        AppLanguage.HINDI -> "उपलब्ध नहीं है"
    }

    fun receiverRunning(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Phone receiver: Running"
        AppLanguage.TELUGU -> "ఫోన్ రిసీవర్: రన్ అవుతోంది"
        AppLanguage.HINDI -> "फ़ोन रिसीवर: चालू है"
    }

    fun receiverStopped(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Phone receiver: Stopped"
        AppLanguage.TELUGU -> "ఫోన్ రిసీవర్: ఆపివేయబడింది"
        AppLanguage.HINDI -> "फ़ोन रिसीवर: बंद है"
    }

    fun espConnectedStatus(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "ESP32 telemetry: Connected"
        AppLanguage.TELUGU -> "ESP32 టెలిమెట్రీ: కనెక్ట్ అయింది"
        AppLanguage.HINDI -> "ESP32 टेलीमेट्री: कनेक्टेड"
    }

    fun espWaitingStatus(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "ESP32 telemetry: Waiting"
        AppLanguage.TELUGU -> "ESP32 టెలిమెట్రీ: వేచి ఉంది"
        AppLanguage.HINDI -> "ESP32 टेलीमेट्री: प्रतीक्षा में"
    }

    fun espDisconnectedStatus(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "ESP32 telemetry: Disconnected"
        AppLanguage.TELUGU -> "ESP32 టెలిమెట్రీ: డిస్‌కనెక్ట్ అయింది"
        AppLanguage.HINDI -> "ESP32 टेलीमेट्री: डिस्कनेक्टेड"
    }

    fun phLevel(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "pH Level"
        AppLanguage.TELUGU -> "పీహెచ్ స్థాయి"
        AppLanguage.HINDI -> "पीएच स्तर"
    }

    fun soilTemp(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Soil Temperature"
        AppLanguage.TELUGU -> "నేల ఉష్ణోగ్రత"
        AppLanguage.HINDI -> "मिट्टी का तापमान"
    }

    fun nutrientStatus(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Nutrient Status"
        AppLanguage.TELUGU -> "పోషకాల స్థితి"
        AppLanguage.HINDI -> "पोषक तत्व स्थिति"
    }

    fun scanSoil(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Scan Soil"
        AppLanguage.TELUGU -> "నేలను స్కాన్ చేయండి"
        AppLanguage.HINDI -> "मिट्टी स्कैन करें"
    }

    fun cropRecommendation(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Crop Suitability"
        AppLanguage.TELUGU -> "పంట అనుకూలత"
        AppLanguage.HINDI -> "फसल उपयुक्तता"
    }

    fun waterGuidance(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Water Guidance"
        AppLanguage.TELUGU -> "నీటి నిర్వహణ"
        AppLanguage.HINDI -> "जल मार्गदर्शन"
    }

    fun nutrientGuidance(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Nutrient Guidance"
        AppLanguage.TELUGU -> "పోషక మార్గదర్శకత్వం"
        AppLanguage.HINDI -> "पोषक तत्व मार्गदर्शन"
    }

    fun pestMonitoring(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Pest Monitoring"
        AppLanguage.TELUGU -> "కీటకాల నిఘా"
        AppLanguage.HINDI -> "कीट निगरानी"
    }

    fun demandIntelligence(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Market Reference"
        AppLanguage.TELUGU -> "మార్కెట్ సమాచారం"
        AppLanguage.HINDI -> "बाजार संदर्भ"
    }

    fun govAdvisory(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Government Reference"
        AppLanguage.TELUGU -> "ప్రభుత్వ సమాచారం"
        AppLanguage.HINDI -> "सरकारी संदर्भ"
    }

    fun finalDecision(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Your Farm Decision"
        AppLanguage.TELUGU -> "మీ పొలం నిర్ణయం"
        AppLanguage.HINDI -> "आपका खेत निर्णय"
    }

    fun whyThisCrop(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Why This Crop?"
        AppLanguage.TELUGU -> "ఈ పంట ఎందుకు?"
        AppLanguage.HINDI -> "यही फसल क्यों?"
    }

    fun decisionLogic(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Decision Logic"
        AppLanguage.TELUGU -> "నిర్ణయ సూత్రం"
        AppLanguage.HINDI -> "निर्णय तर्क"
    }

    fun scanTimeoutError(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "No fresh ESP32 reading received. Check Wi-Fi, IP, port and token."
        AppLanguage.TELUGU -> "కొత్త ESP32 రీడింగ్ అందలేదు. Wi-Fi, IP, పోర్ట్ మరియు టోకెన్ తనిఖీ చేయండి."
        AppLanguage.HINDI -> "कोई नई ESP32 रीडिंग प्राप्त नहीं हुई। वाई-फाई, आईपी, पोर्ट और टोकन जांचें।"
    }
}
