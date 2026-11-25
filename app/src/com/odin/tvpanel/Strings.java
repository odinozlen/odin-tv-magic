package com.odin.tvpanel;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * English / Turkish texts for API responses and the TV screens.
 * The web panel sends its language in the X-Lang header; the TV screens follow the system language.
 */
final class Strings {
    private static final Map<String, String[]> T = new HashMap<>();

    private static void put(String key, String en, String tr) {
        T.put(key, new String[]{en, tr});
    }

    static {
        // generic
        put("ok", "Done", "Tamam");
        put("error", "Something went wrong: %s", "Bir şeyler ters gitti: %s");
        put("not_found", "Not found", "Bulunamadı");
        put("bad_request", "Invalid request", "Geçersiz istek");
        put("too_large", "Request too large", "İstek çok büyük");
        put("forbidden_host", "Open the panel with the TV's IP address", "Paneli TV'nin IP adresiyle aç");

        // auth / pairing of phones
        put("auth.required", "This phone is not paired. Open Odin TV Magic on the TV and scan the QR code.",
                "Bu telefon eşleşmemiş. TV'de Odin TV Magic'i açıp karekodu okut.");
        put("auth.locked", "Too many failed attempts. Try again in %d min.", "Çok fazla hatalı deneme. %d dk sonra tekrar dene.");
        put("auth.clock", "Clock mismatch, retrying", "Saat farkı, tekrar deneniyor");
        put("pair.ok", "Paired", "Eşleşti");
        put("pair.bad_code", "Wrong or expired code. Open Odin TV Magic on the TV for a new one.",
                "Kod yanlış ya da süresi doldu. TV'de Odin TV Magic'i açınca yeni kod çıkar.");
        put("device.revoked", "Phone removed", "Telefon kaldırıldı");

        // adb
        put("adb.not_connected", "Not connected yet", "Henüz bağlanılmadı");
        put("adb.connected", "Connected", "Bağlı");
        put("adb.waiting_approval", "Approve \"Allow debugging?\" on the TV", "TV'de \"Hata ayıklamaya izin verilsin mi?\" kutusunu onayla");
        put("adb.not_approved", "Debugging was not approved on the TV. Use \"Check again\" to show the prompt again.",
                "TV'de hata ayıklama onaylanmadı. Pencereyi tekrar göstermek için \"Tekrar kontrol et\"e bas.");
        put("tv.burned", "Too many wrong codes were tried. Close and reopen this screen for a new code.",
                "Çok fazla yanlış kod denendi. Yeni kod için bu ekranı kapatıp tekrar aç.");
        put("tv.forget_confirm", "Remove every paired phone? They will need to scan the QR code again.",
                "Eşleşmiş bütün telefonlar kaldırılsın mı? Tekrar karekodu okutmaları gerekir.");
        put("tv.cancel", "Cancel", "Vazgeç");
        put("adb.rejected", "The TV rejected the debugging key", "TV hata ayıklama anahtarını reddetti");
        put("adb.refused", "ADB is off. Turn on network/wireless debugging (see setup).",
                "ADB kapalı. Ağ ya da kablosuz hata ayıklamayı aç (kuruluma bak).");
        put("adb.wireless_off", "Wireless debugging is off", "Kablosuz hata ayıklama kapalı");
        put("adb.failed", "Could not connect to ADB", "ADB'ye bağlanılamadı");
        put("debloat.not_allowed", "Only apps from the suggestion list can be turned off here.",
                "Buradan yalnızca öneri listesindeki uygulamalar kapatılabilir.");
        put("adb.paired", "Paired with wireless debugging", "Kablosuz hata ayıklama ile eşleşti");
        put("pair.needs_android12", "Wireless debugging pairing needs Android 12 or newer",
                "Kablosuz hata ayıklama eşleştirmesi Android 12 veya üstü gerektirir");
        put("pair.bad_code_format", "Enter the 6-digit code", "6 haneli kodu gir");
        put("pair.wrong_code", "Wrong pairing code", "Eşleştirme kodu yanlış");
        put("pair.failed", "Pairing failed, try again", "Eşleştirme başarısız, tekrar dene");
        put("pair.no_port", "Pairing port not found. Keep the \"Pair device with pairing code\" window open on the TV.",
                "Eşleştirme portu bulunamadı. TV'de \"Eşleştirme koduyla cihaz eşle\" penceresini açık tut.");
        put("mdns.not_found", "Wireless debugging port not found", "Kablosuz hata ayıklama portu bulunamadı");

        // actions
        put("act.home", "Home", "Ana ekran");
        put("act.sleep", "TV is going to sleep", "TV uykuya alınıyor");
        put("act.reboot", "TV is restarting, the panel is back in ~1 min", "TV yeniden başlıyor, panel ~1 dk sonra döner");
        put("act.unknown", "Unknown action", "Bilinmeyen işlem");
        put("launch.ok", "Opening %s", "%s açılıyor");
        put("launch.unknown", "App not installed", "Uygulama yüklü değil");
        put("source.ok", "Switching to %s", "%s açılıyor");
        put("source.unknown", "Unknown input", "Bilinmeyen giriş");
        put("clean.ok", "+%d MB freed", "+%d MB boşaldı");
        put("clean.closed", "+%d MB freed · closed %s", "+%d MB boşaldı · kapatılanlar: %s");

        // remote / keyboard / volume
        put("key.unknown", "Unknown key", "Bilinmeyen tuş");
        put("kb.empty", "Nothing to send", "Gönderilecek metin yok");
        put("kb.too_long", "Up to 200 characters", "En fazla 200 karakter");
        put("kb.sent", "Sent", "Gönderildi");
        put("kb.ascii_only", "Sent (special characters need the Philips pairing)", "Gönderildi (özel karakterler için Philips eşleştirmesi gerekir)");
        put("vol.muted", "Muted", "Ses kapalı");
        put("vol.up", "Volume up", "Ses açıldı");
        put("vol.down", "Volume down", "Ses kısıldı");
        put("vol.set", "Volume %d", "Ses %d");
        put("vol.unavailable", "This TV does not report its volume; use the − and + buttons.", "Bu TV ses seviyesini bildirmiyor; − ve + düğmelerini kullan.");

        // screenshot
        put("shot.protected", "Protected content (e.g. Netflix) is on screen; the TV returns no image",
                "Ekranda korumalı içerik var (Netflix gibi); TV görüntü vermiyor");
        put("shot.decode", "Could not decode the screenshot", "Ekran görüntüsü çözülemedi");

        // Philips
        put("philips.not_paired", "Pair with the Philips TV first", "Önce Philips TV ile eşleştir");
        put("philips.pin", "Enter the PIN shown on the TV", "TV ekranındaki PIN'i gir");
        put("philips.paired", "Philips features connected", "Philips özellikleri bağlandı");
        put("philips.pin_wrong", "PIN not accepted", "PIN kabul edilmedi");
        put("philips.unsupported", "Only available on Philips Android TVs", "Yalnızca Philips Android TV'lerde var");
        put("philips.unreachable", "This TV's Philips interface (JointSpace) does not answer, so Ambilight and picture can only be set with the TV's remote.",
                "Bu TV'nin Philips arayüzü (JointSpace) yanıt vermiyor; Ambilight ve görüntü yalnızca TV kumandasıyla ayarlanabilir.");
        put("philips.pair_refused", "The TV refused pairing (%s). Tap \"Show PIN on TV\" again.",
                "TV eşleştirmeyi reddetti (%s). \"TV'de PIN göster\"e tekrar bas.");
        put("amb.unsupported", "This TV reports no Ambilight", "Bu TV Ambilight bildirmiyor");
        put("amb.no_effect", "The TV accepted the command but Ambilight did not change. Send the diagnostics (Picture tab) with your report.",
                "TV komutu aldı ama Ambilight değişmedi. Bildirirken tanı bilgisini de gönder (Görüntü sekmesi).");
        put("amb.on", "Ambilight on", "Ambilight açık");
        put("amb.off", "Ambilight off", "Ambilight kapalı");
        put("amb.video", "Ambilight follows video", "Ambilight videoyu takip ediyor");
        put("amb.music", "Ambilight follows music", "Ambilight müziği takip ediyor");
        put("amb.color", "Color set", "Renk ayarlandı");
        put("pic.style", "Picture style: %s", "Görüntü tarzı: %s");
        put("pic.style_missing", "\"%s\" is not available for the current content, list updated",
                "Şu anki içerikte \"%s\" yok, liste güncellendi");
        put("pic.style_failed", "Could not change the picture style", "Görüntü tarzı değiştirilemedi");
        put("pic.menu_failed", "Could not open the quick menu", "Hızlı menü açılamadı");
        put("pic.menu_missing", "This TV has no quick menu the panel can open, so picture styles are changed with the TV's remote. Brightness works.",
                "Bu TV'de panelin açabileceği bir hızlı menü yok; görüntü tarzı TV kumandasıyla değiştirilir. Parlaklık çalışır.");
        put("pic.list_failed", "Could not read the picture styles", "Görüntü tarzları okunamadı");
        put("pic.brightness", "Brightness %d", "Parlaklık %d");
        put("pic.brightness_denied", "The TV did not accept the brightness change (now %d)",
                "TV parlaklık değişikliğini kabul etmedi (şu an %d)");

        // watchdog
        put("wd.never", "not run yet", "henüz çalışmadı");
        put("wd.ok", "logger %d threads, all good", "logger %d thread, sorun yok");
        put("wd.reset", "logger %d threads → restarted", "logger %d thread → sıfırlandı");
        put("wd.error", "error: %s", "hata: %s");

        // party mode
        put("party.off", "Party mode is off. Turn it on in Settings.", "Sürpriz modu kapalı. Ayarlar'dan aç.");
        put("party.started", "Show started", "Gösteri başladı");
        put("party.running", "The show is already running", "Gösteri zaten çalışıyor");
        put("party.stopped", "Stopped", "Durduruldu");
        put("party.default_msg", "This TV is under Odin's control 😎", "Bu TV Odin'in kontrolünde 😎");

        put("home.ok", "Home screen: %s", "Ana ekran: %s");
        put("home.unknown", "That home screen is not installed", "Bu ana ekran yüklü değil");
        put("home.failed", "The TV did not accept the new home screen; nothing was changed",
                "TV yeni ana ekranı kabul etmedi; hiçbir şey değiştirilmedi");
        put("home.store", "%s: the Play Store page is open on the TV, press Install there", "%s: TV'de Play Store sayfası açıldı, oradan Yükle'ye bas");
        put("home.restored", "Original home screen restored", "Orijinal ana ekran geri getirildi");
        put("addon.installed", "Already installed", "Zaten yüklü");
        put("addon.busy", "Installation is already running", "Kurulum zaten sürüyor");
        put("addon.started", "Downloading…", "İndiriliyor…");
        put("addon.failed", "Installation failed", "Kurulum başarısız oldu");
        put("addon.verify", "The download did not match the expected file; nothing was installed",
                "İndirilen dosya beklenenle eşleşmedi; hiçbir şey kurulmadı");
        put("addon.abi", "This app does not support this TV's processor", "Bu uygulama TV'nin işlemcisini desteklemiyor");
        put("debloat.limit", "Choose 1 to 10 apps at a time", "Bir seferde 1 ile 10 arası uygulama seç");
        put("debloat.disabled", "%d apps turned off. Check inputs, sound and the remote.",
                "%d uygulama kapatıldı. Kaynakları, sesi ve kumandayı bir dene.");
        put("debloat.enabled", "%d apps turned back on", "%d uygulama tekrar açıldı");
        put("settings.saved", "Saved", "Kaydedildi");

        // TV screen (MainActivity)
        put("tv.title", "Odin TV Magic", "Odin TV Magic");
        put("tv.scan", "Scan with your phone camera", "Telefon kamerasıyla okut");
        put("tv.or_open", "or open %s and enter the code", "ya da %s adresini açıp kodu gir");
        put("tv.code", "Pairing code", "Eşleştirme kodu");
        put("tv.code_valid", "valid while this screen is open", "bu ekran açıkken geçerli");
        put("tv.adb", "ADB: %s", "ADB: %s");
        put("tv.approve_hint", "(not visible? press Back once; still nothing: turn USB debugging off and on again)",
                "(görünmüyorsa bir kez Geri'ye bas; yine çıkmazsa USB hata ayıklamayı kapatıp tekrar aç)");
        put("tv.philips", "Philips features: %s", "Philips özellikleri: %s");
        put("tv.philips_on", "connected", "bağlı");
        put("tv.philips_off", "not paired (pair from the panel)", "eşleşmedi (panelden eşleştir)");
        put("tv.devices", "Paired phones: %d", "Eşleşmiş telefonlar: %d");
        put("tv.forget", "Forget all phones", "Tüm telefonları unut");
        put("tv.forgotten", "All phones removed", "Tüm telefonlar kaldırıldı");
        put("tv.background", "You can close this screen; the panel keeps running in the background.",
                "Bu ekranı kapatabilirsin; panel arka planda çalışmaya devam eder.");
        put("tv.warning", "Anyone who can open this screen can pair a phone. Never accept unknown \"Allow debugging?\" prompts.",
                "Bu ekranı açabilen herkes telefon eşleyebilir. Tanımadığın \"Hata ayıklamaya izin ver\" kutularını asla onaylama.");
        put("tv.starting", "Starting…", "Başlatılıyor…");
        put("vol.needs_setup", "This TV passes the volume to the TV/soundbar; finish the setup on the TV (open Odin TV Magic) to control it.",
                "Bu cihaz sesi TV'ye/ses sistemine iletiyor; kontrol etmek için TV'deki kurulumu tamamla (Odin TV Magic'i aç).");
        put("tv.pair_short", "Pair your phone: scan the QR code, or open %s", "Telefonu eşle: karekodu okut ya da %s adresini aç");
        put("tv.basic_now", "You can pair your phone already (volume and play/pause work right away). This setup unlocks the rest.",
                "Telefonu şimdiden eşleyebilirsin (ses ve oynat/duraklat hemen çalışır). Bu kurulum gerisini açar.");
        put("tv.basic_now.philips", "You can pair your phone already: after the Philips PIN in the panel, the remote, volume, apps and Ambilight work right away. This setup unlocks the rest.",
                "Telefonu şimdiden eşleyebilirsin: paneldeki Philips PIN'inden sonra kumanda, ses, uygulamalar ve Ambilight hemen çalışır. Bu kurulum gerisini açar.");
        put("tv.setup_done", "Setup complete", "Kurulum tamam");

        // TV setup guide (Setup)
        put("setup.step", "SETUP · STEP %d OF 3", "KURULUM · ADIM %d/3");
        put("setup.t.dev_off", "Turn on developer mode", "Geliştirici modunu aç");
        put("setup.t.adb_off", "Turn on USB debugging", "USB hata ayıklamayı aç");
        put("setup.t.net_off", "Turn on network debugging", "Ağ üzerinden hata ayıklamayı aç");
        put("setup.t.checking", "Connecting to the TV…", "TV'ye bağlanılıyor…");
        put("setup.t.approve", "Approve the connection", "Bağlantıyı onayla");
        put("setup.b.dev_off", "The button opens the About screen. Go to \"Build\" (or \"Android TV OS build\") and press OK 7 times. When it says you are a developer, this screen comes back by itself (otherwise press Back).",
                "Düğme Hakkında ekranını açar. \"Derleme\" (ya da \"Android TV OS derlemesi\") satırına gel ve OK'e 7 kez bas. \"Artık geliştiricisiniz\" yazınca bu ekran kendiliğinden geri gelir (gelmezse Geri'ye bas).");
        put("setup.b.dev_off.fire_tv", "The button opens the About screen. Go to the first row (your device name, e.g. \"Fire TV Stick\") and press OK 7 times. When it says you are a developer, press Back.",
                "Düğme Hakkında ekranını açar. En üstteki satıra (cihazın adı, ör. \"Fire TV Stick\") gel ve OK'e 7 kez bas. \"Artık geliştiricisiniz\" yazınca Geri'ye bas.");
        put("setup.b.adb_off", "The button opens Developer options. Turn on \"USB debugging\" and confirm. If there is also a \"Network debugging\" switch, turn it on too.",
                "Düğme Geliştirici seçeneklerini açar. \"USB hata ayıklama\"yı aç ve onayla. \"Ağ üzerinden hata ayıklama\" anahtarı da varsa onu da aç.");
        put("setup.b.adb_off.fire_tv", "The button opens Developer options. Turn on \"ADB debugging\" and confirm.",
                "Düğme Geliştirici seçeneklerini açar. \"ADB hata ayıklama\"yı aç ve onayla.");
        put("setup.b.net_off", "USB debugging is on, but this TV does not accept the connection over the network. In Developer options turn on \"Network debugging\" (ADB over network). If there is no such switch: turn on \"Wireless debugging\", choose \"Pair device with pairing code\" and enter the code in the phone panel (Settings › TV connection).",
                "USB hata ayıklama açık ama bu TV bağlantıyı ağ üzerinden kabul etmiyor. Geliştirici seçeneklerinde \"Ağ üzerinden hata ayıklama\"yı (ADB over network) aç. Böyle bir anahtar yoksa: \"Kablosuz hata ayıklama\"yı aç, \"Eşleştirme koduyla cihaz eşle\"yi seç ve kodu telefondaki panele gir (Ayarlar › TV bağlantısı).");
        put("setup.b.net_off_old", "USB debugging is on, but this TV does not accept the connection over the network. In Developer options turn on \"Network debugging\" (ADB over network). If there is no such switch, it can be turned on once from a computer: adb tcpip 5555 (see the README).",
                "USB hata ayıklama açık ama bu TV bağlantıyı ağ üzerinden kabul etmiyor. Geliştirici seçeneklerinde \"Ağ üzerinden hata ayıklama\"yı (ADB over network) aç. Böyle bir anahtar yoksa bir kez bilgisayardan açılabilir: adb tcpip 5555 (README'ye bak).");
        put("setup.b.checking", "One moment.", "Bir saniye.");
        put("setup.b.approve", "The TV shows \"Allow debugging?\": tick \"Always allow\" and press \"Allow\". Not visible? Press the button. Still nothing: turn USB debugging off and on again.",
                "TV'de \"Hata ayıklamaya izin verilsin mi?\" penceresi çıkar: \"Her zaman izin ver\"i işaretle ve \"İzin ver\"e bas. Görünmüyorsa düğmeye bas; yine çıkmazsa USB hata ayıklamayı kapatıp tekrar aç.");
        put("setup.path.about.android_tv", "If the button does not work: Settings › Device Preferences › About",
                "Düğme çalışmazsa: Ayarlar › Cihaz Tercihleri › Hakkında");
        put("setup.path.about.google_tv", "If the button does not work: Settings › System › About",
                "Düğme çalışmazsa: Ayarlar › Sistem › Hakkında");
        put("setup.path.about.fire_tv", "If the button does not work: Settings › My Fire TV › About",
                "Düğme çalışmazsa: Ayarlar › Fire TV'm › Hakkında");
        put("setup.path.dev.android_tv", "If the button does not work: Settings › Device Preferences › Developer options",
                "Düğme çalışmazsa: Ayarlar › Cihaz Tercihleri › Geliştirici seçenekleri");
        put("setup.path.dev.google_tv", "If the button does not work: Settings › System › Developer options",
                "Düğme çalışmazsa: Ayarlar › Sistem › Geliştirici seçenekleri");
        put("setup.path.dev.fire_tv", "If the button does not work: Settings › My Fire TV › Developer options",
                "Düğme çalışmazsa: Ayarlar › Fire TV'm › Geliştirici seçenekleri");
        put("setup.btn.about", "Open About", "Hakkında ekranını aç");
        put("setup.btn.dev", "Open Developer options", "Geliştirici seçeneklerini aç");
        put("setup.btn.prompt", "Show the window again", "Pencereyi tekrar göster");
        put("setup.no_settings", "This TV did not open the screen. Use the path below.", "TV bu ekranı açmadı. Aşağıdaki yolu kullan.");
        put("tv.error", "Error: %s", "Hata: %s");

        // Party mode screen
        put("hk.l1", "[*] target found: %s", "[*] hedef bulundu: %s");
        put("hk.l2", "[*] device: %s · Android %s · kernel %s", "[*] cihaz: %s · Android %s · çekirdek %s");
        put("hk.l3", "[*] %d CPU cores · %s MB memory", "[*] %d CPU çekirdeği · %s MB bellek");
        put("hk.l4", "[~] bypassing firewall  %BAR%", "[~] güvenlik duvarı atlatılıyor  %BAR%");
        put("hk.l5", "[~] cracking encryption  %HEX%", "[~] şifreleme kırılıyor  %HEX%");
        put("hk.l6", "[+] root access ................ GRANTED ✓", "[+] kök erişimi ................ VERİLDİ ✓");
        put("hk.l7", "[+] ambilight controller ....... HIJACKED ✓", "[+] ambilight denetleyicisi ..... ELE GEÇİRİLDİ ✓");
        put("hk.l7b", "[+] display controller ......... HIJACKED ✓", "[+] ekran denetleyicisi ......... ELE GEÇİRİLDİ ✓");
        put("hk.l8", "[+] remote signal .............. CLONED ✓", "[+] kumanda sinyali ............. KOPYALANDI ✓");
        put("hk.l9", "[!] REMOTE CONTROL ACTIVE", "[!] UZAKTAN KONTROL AKTİF");
        put("hk.alert", "⚠ SYSTEM BREACH DETECTED ⚠", "⚠ SİSTEM İHLALİ TESPİT EDİLDİ ⚠");
        put("hk.unauth", "unauthorized access: 192.168.x.x  ·  source unknown", "yetkisiz erişim: 192.168.x.x  ·  kaynak bilinmiyor");
        put("hk.takeover", "CONTROL TAKEN", "KONTROL ELE GEÇİRİLDİ");
        put("hk.restore", "system returning to normal", "sistem normale dönüyor");
    }

    private Strings() {
    }

    static boolean isTurkish(String lang) {
        return lang != null && lang.toLowerCase(Locale.ROOT).startsWith("tr");
    }

    static String systemLang() {
        return Locale.getDefault().getLanguage();
    }

    static String t(String lang, String key, Object... args) {
        String[] v = T.get(key);
        if (v == null) return key;
        String s = v[isTurkish(lang) ? 1 : 0];
        return args.length == 0 ? s : String.format(Locale.ROOT, s, args);
    }

    static boolean has(String key) {
        return key != null && T.containsKey(key);
    }

    /** TV-side text in the system language. */
    static String tv(String key, Object... args) {
        return t(systemLang(), key, args);
    }
}
