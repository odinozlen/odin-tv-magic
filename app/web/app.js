"use strict";
/* Odin TV Magic: phone UI. Served by the app on the TV; talks only to that TV. */

/* ---------------- author ---------------- */
const AUTHOR = { name: "Odin Enes Özlen", handle: "odinozlen", email: "hi@odinozlen.com", repo: "https://github.com/odinozlen/odin-tv-magic" };

/* ---------------- i18n ---------------- */
const I18N = {
  en: {
    connecting: "connecting…", online: "online", offline: "unreachable", restarting: "restarting", sleeping: "asleep",
    "now": "Now: ", "tv.off": "TV is off or not on this network", home_screen: "Home screen",
    "setup.title": "Finish the setup on the TV",
    "setup.basic": "Volume and play/pause already work. For the remote, apps, cleanup, inputs, screenshots and the TV tools, open <b>Odin TV Magic</b> on the TV: it walks you through the setup step by step (about a minute with the remote).",
    "setup.basic.ph": "The remote, volume, apps and Ambilight already work. For cleanup, inputs, screenshots and the TV tools, open <b>Odin TV Magic</b> on the TV: it walks you through the setup step by step (about a minute with the remote).",
    "setup.manual": "Set it up by hand",
    "needs.setup": "Needs the setup on the TV: open Odin TV Magic there",
    "basic.remote": "The other remote keys need the setup on the TV: open Odin TV Magic there.",
    "setup.intro": "The panel controls the TV through Android's debugging bridge (ADB). Turn it on once:",
    "setup.s1": "On the TV: <b>Settings › Device Preferences › About</b>, press <b>Build</b> 7 times.",
    "setup.s2": "Open <b>Developer options</b> and turn on <b>USB debugging</b>.",
    "setup.classic": "Option A · USB / Network debugging (most TVs)",
    "setup.a1": "On most TVs <b>USB debugging</b> alone is enough. If there is also a <b>Network debugging</b> (ADB over network) switch, turn it on too.",
    "setup.a2": "An <b>Allow debugging?</b> window appears on the TV: tick <b>Always allow</b>, press <b>Allow</b>.",
    "setup.wireless": "Option B · Wireless debugging (if Option A does not connect)",
    "setup.b1": "Turn on <b>Wireless debugging</b> and allow it on this network.",
    "setup.b2": "Choose <b>Pair device with pairing code</b> and enter the 6-digit code here:",
    "setup.pair": "Pair", "setup.retry": "Check again",
    uptime: "TV on for", free: "%s MB free of %s MB", used: "used", logger: "Philips logger",
    clean: "Free up memory", "fl.val": "%s / 500", "ram.title": "Memory",
    "vol.ext": "TV / soundbar", "np.home": "Home", "np.label": "Playing now", "pad.buttons": "Buttons", "pad.touch": "Touchpad", "tpad.hint": "Swipe to move · tap for OK",
    "favs.lead": "Drag ≡ to change the order.", "favs.more": "More apps", "favs.none": "No apps on the home tab yet.",
    "ob.skipall": "Skip", "ob.next": "Continue", "ob.later": "Not now", "ob.done": "Start",
    "ob.tv.title": "Unlock everything", "ob.tv.text": "Odin TV Magic on the TV shows the setup step by step: developer mode, USB debugging, then <b>Allow</b>. About a minute with the remote. You can also do it later.",
    "ob.tv.ok": "Connected to the TV", "ob.tv.check": "Check again",
    "ob.ph.title": "Ambilight and Philips features", "ob.ph.text": "Ambilight, picture style, brightness and an instant remote need a one-time pairing. Tap the button: the TV shows a 4-digit PIN, type it here.",
    "ob.home.title": "Add the remote to your phone", "ob.home.why": "Next time it opens with one tap, full screen like an app, no QR code needed.",
    "ob.apps.title": "Your apps", "ob.apps.text": "Tap the apps you want on the home tab.",
    "ob.done.title": "All set", "ob.done.text": "Remote, apps and settings are one tap away.",
    "src.home": "TV home", "src.home.sub": "Back to apps", "src.home.ok": "Home screen",
    "src.tv.sub": "Live TV", "clean.hint": "Closes apps waiting in the background. What is on screen keeps playing.",
    "src.back": "\"TV home\" takes you back",
    "a2hs.title": "Add the remote to your phone", "a2hs.why": "Next time it opens with one tap, full screen like an app, no QR code needed.",
    "a2hs.ios": "<li>In Safari, tap <b>Share</b> <span class=\"ios-share\"></span> (bottom of the screen).</li><li>Choose <b>Add to Home Screen</b>, then <b>Add</b>.</li>",
    "a2hs.android": "<li>In Chrome, tap <b>⋮</b> (top right).</li><li>Choose <b>Add to Home screen</b> (or <b>Install app</b>), then <b>Add</b>.</li>",
    "desk.title": "This panel is made for your phone",
    "desk.text": "Odin TV Magic is a TV remote and settings panel <b>for your phone</b>. The computer was only needed to install it.",
    "desk.s1": "Open <b>Odin TV Magic</b> on the TV.",
    "desk.s2": "Scan the QR code on the TV with your <b>phone camera</b>.",
    "desk.s3": "On the phone, add it to the home screen: it then opens like an app.",
    "desk.anyway": "Continue on this computer anyway",
    "a2hs.ok": "Got it",
    "made.by": "Made by %s",
    "upgrade.title": "Upgrade your TV", "upgrade.sub": "Add features your TV doesn't have.",
    "speed.lead": "Pre-installed apps you never open still run in the background and use memory.",
    "speed.row": "Turn off unused apps", "speed.sub.n": "%s suggestions · %s turned off",
    "airplay.builtin": "Built into this TV", "airplay.add": "Add",
    "home.atv": "Android TV home (original)", "home.gtv": "Google TV home (original)",
    "home.note": "On some TVs (e.g. Philips) the original home screen is turned off so your choice sticks. \"Restore\" turns it back on.",
    "speed.title": "Speed up your TV", "speed.sub": "Turn off pre-installed apps you don't use",
    "speed.intro": "Turn off pre-installed apps you don't use. Nothing is deleted: you can turn every app back on here.",
    "speed.go": "Turn off selected", "speed.go.n": "Turn off %s apps", "speed.off": "Turned off", "speed.on": "Turn on",
    "speed.none": "Nothing left to suggest.", "speed.confirm": "Turn off %s apps? Afterwards check inputs, sound and the remote. You can turn them back on here.",
    "speed.max": "At most %s at a time",
    "g.telemetry": "Tracking & logging", "g.unused": "Rarely used", "g.online": "Philips online services",
    "g.regional": "Pre-installed regional apps", "g.preinstalled": "Pre-installed apps", "g.launcher": "Old home screen parts",
    "look.title": "Change your TV's look", "look.sub": "Switch to a clean home screen without ads and recommendations.",
    "home.install": "Install", "launcher.projectivy": "Customizable, fast, no ads. Most popular choice.",
    "launcher.flauncher": "Open source, minimal and free.",
    addons: "Extras", "airplay.desc": "Show your iPhone, iPad or Mac screen on the TV. ~%s MB.",
    "airplay.title": "AirPlay (screen mirroring)",
    "airplay.installed": "Installed", "airplay.dl": "Downloading %s%", "airplay.installing": "Installing…", "airplay.open": "Open",
    "airplay.starting": "Starting…", "airplay.on": "On", "airplay.start": "Turn on",
    "airplay.off": "Installed but off: iPhones cannot see the TV.",
    "airplay.how": "<b>iPhone / iPad / Mac:</b> Control Center › Screen Mirroring › <b>%s</b> (same Wi-Fi).",
    apps: "Apps", sources: "Sources", tools: "Tools", keyboard: "Keyboard", screen: "Screen", sleep: "Sleep", restart: "Restart",
    "no.apps": "No apps selected. Pick some in Settings.",
    volume: "Volume", muted: "Muted", "pad.hint": "Tap, swipe or hold", back: "Back", home: "Home", menu: "Menu", type: "Type with keyboard",
    "philips.title": "Pair once for Ambilight",
    "philips.intro": "Ambilight, picture style, brightness and an instant remote need a one-time pairing with the TV: tap the button, the TV shows a 4-digit PIN, type it here.",
    "philips.unreachable": "This TV's Philips interface (JointSpace) does not answer, so Ambilight and picture can only be set with the TV's remote.",
    "amb.need_pair": "Ambilight, picture style and brightness work after a one-time pairing with the TV: tap the button, the TV shows a 4-digit PIN, type it on the phone.",
    "amb.pair_first": "Pair first: tap \"Show PIN on TV\"",
    "diag.open": "Philips diagnostics", "diag.hint": "What the TV reports about its Philips interface. No addresses, names or keys. Copy it into your bug report.",
    "diag.copy": "Copy", "diag.copied": "Copied", "diag.select": "Selected: copy it with your phone's menu",
    "philips.start": "Show PIN on TV", "philips.enter": "Enter the PIN shown on the TV.", "philips.connect": "Connect",
    "pic.style": "Picture style", brightness: "Brightness", "b.night": "Night", "b.evening": "Evening", "b.day": "Day", "b.max": "Max",
    on: "On", off: "Off", "amb.video": "Video", "amb.music": "Music",
    "pic.applying": "Applying %s… the TV shows its quick menu for a few seconds",
    "pic.read": "Read styles from TV", "pic.none": "List not read yet",
    "pic.no_menu": "On this TV the picture style is changed with the TV's remote; brightness works here.",
    "scene": "Cinema", "scene.desc": "%s + Ambilight", "scene.busy": "Setting up cinema mode…", "scene.ok": "Cinema mode ready",
    lang: "Language", favs: "Apps on the home tab", "styles.show": "Picture styles to show",
    party: "Party mode", "party.name": "Surprise",
    "party.desc": "A ~45 s \"the TV got hacked\" show for friends: alarm, hacker terminal, apps opening by themselves. Everything goes back to normal afterwards.",
    "party.sheet": "The TV acts \"hacked\": alarm, hacker terminal, apps opening by themselves. Your message appears at the end. About 45 seconds; you can stop it any time.",
    "party.msg": "This TV is under Odin's control 😎", "party.label": "> SURPRISE_", "party.running": "> RUNNING… STOP", "party.stop": "tap to put everything back",
    "party.go": "START", "party.started": "Show started", "party.stopped": "Back to normal",
    phones: "Paired phones", "this.phone": "this phone", remove: "Remove", adb: "TV connection",
    "adb.via": "Status: %s · transport: %s", about: "About",
    security: "Only phones paired on the TV screen can use the panel. Requests are signed; the key never leaves this phone. Use it only on your home network.",
    unpair: "Unpair this phone", "unpair.confirm": "Unpair this phone? You will need the TV screen to pair again.",
    "tab.home": "Home", "tab.remote": "Remote", "tab.picture": "Picture", "tab.settings": "Settings",
    "kb.hint": "Select a text field on the TV, type here and send. Handy for Netflix or YouTube search.", send: "Send",
    "shot.title": "Screenshot", "shot.hint": "Press and hold the image to save it. Protected content (e.g. Netflix) gives no image.", refresh: "Refresh",
    "pair.title": "Pair this phone",
    "pair.how": "On the TV open <b>Odin TV Magic</b> and scan the QR code with your phone camera, or enter the 6-digit code shown there.",
    "pair.go": "Pair", "pair.short": "Enter the 6-digit code", "paired": "Paired",
    "confirm.reboot": "Restart the TV?", "confirm.sleep": "Put the TV to sleep? The panel is unavailable until the TV is on again.",
    "hours": "%s h %s min", "days": "%s d %s h", "mins": "%s min",
    "philips.need": "Pair Philips features first (Picture tab)",
    "styles.hint": "Hidden styles stay available on the TV remote.",
    "home.title": "Home screen", "home.stock": "Also turn off the stock launcher",
    "home.stock.desc": "Needed on some TVs (e.g. Philips) so the Home button opens your choice. \"Restore\" turns it back on.",
    "home.restore": "Restore original home screen", "home.use": "Use", "home.current": "Current", "home.stockname": "Original",
    "home.none": "Only the stock home screen is installed. Install Projectivy Launcher or FLauncher from the Play Store, then choose it here.",
    "home.gtvnote": "Google TV: the original home stays on (turning it off can require a factory reset). If the Home button still opens Google TV, enable the \"override\" option inside your launcher's settings.",
    "home.confirm": "Make %s the home screen?", "home.restore.confirm": "Go back to the original home screen?",
    "upd.title": "Odin TV Magic %s is ready", "upd.text": "You have %s. Tap Update: the TV downloads it, checks it and installs it. Paired phones stay paired.",
    "upd.go": "Update", "upd.notes": "What's new", "upd.confirm": "Update Odin TV Magic to %s? The panel is gone for about a minute while the TV installs it.",
    "upd.basic": "Updating from the panel needs the setup on the TV. Without it: on the TV open Downloader, type odinozlen.com/tv and install over the old version (your settings stay).",
    "upd.dl": "Downloading… %s%", "upd.verifying": "Checking the download…", "upd.installing": "Installing… the panel comes back by itself in a moment.",
    "upd.restarting": "The TV is installing the update. The panel reloads by itself when it is back.",
    "upd.failed": "The update failed. Nothing was changed.", "upd.done": "Updated to %s",
    "upd.section": "Updates", "upd.auto": "Automatic update check",
    "upd.check": "Check now", "upd.off": "Update check is off", "upd.never": "Not checked yet",
    "upd.current": "Up to date (%s) · checked %s", "upd.avail": "%s is available",
  },
  tr: {
    connecting: "bağlanıyor…", online: "çevrimiçi", offline: "ulaşılamıyor", restarting: "yeniden başlıyor", sleeping: "uykuda",
    "now": "Şu an: ", "tv.off": "TV kapalı ya da bu ağda değil", home_screen: "Ana ekran",
    "setup.title": "Kurulumu TV'de tamamla",
    "setup.basic": "Ses ve oynat/duraklat şimdiden çalışıyor. Kumanda, uygulamalar, temizlik, kaynaklar, ekran görüntüsü ve TV araçları için TV'de <b>Odin TV Magic</b>'i aç: kurulumu adım adım gösterir (kumandayla yaklaşık bir dakika).",
    "setup.basic.ph": "Kumanda, ses, uygulamalar ve Ambilight şimdiden çalışıyor. Temizlik, kaynaklar, ekran görüntüsü ve TV araçları için TV'de <b>Odin TV Magic</b>'i aç: kurulumu adım adım gösterir (kumandayla yaklaşık bir dakika).",
    "setup.manual": "Elle kurulum",
    "needs.setup": "TV'deki kurulum gerekli: orada Odin TV Magic'i aç",
    "basic.remote": "Diğer kumanda tuşları için TV'deki kurulumu tamamla: orada Odin TV Magic'i aç.",
    "setup.intro": "Panel, TV'yi Android'in hata ayıklama köprüsü (ADB) ile yönetir. Bir kez aç:",
    "setup.s1": "TV'de: <b>Ayarlar › Cihaz Tercihleri › Hakkında</b>, <b>Derleme</b>'ye 7 kez bas.",
    "setup.s2": "<b>Geliştirici seçenekleri</b>'ni aç, <b>USB hata ayıklama</b>'yı etkinleştir.",
    "setup.classic": "Seçenek A · USB / Ağ üzerinden hata ayıklama (çoğu TV)",
    "setup.a1": "Çoğu TV'de yalnızca <b>USB hata ayıklama</b> yeterli. <b>Ağ üzerinden hata ayıklama</b> (ADB over network) anahtarı da varsa onu da aç.",
    "setup.a2": "TV'de <b>Hata ayıklamaya izin verilsin mi?</b> penceresi çıkar: <b>Her zaman izin ver</b>'i işaretle, <b>İzin ver</b>'e bas.",
    "setup.wireless": "Seçenek B · Kablosuz hata ayıklama (A ile bağlanmazsa)",
    "setup.b1": "<b>Kablosuz hata ayıklama</b>'yı aç ve bu ağda izin ver.",
    "setup.b2": "<b>Eşleştirme koduyla cihaz eşle</b>'yi seç, çıkan 6 haneli kodu buraya gir:",
    "setup.pair": "Eşle", "setup.retry": "Tekrar kontrol et",
    uptime: "TV açık kalma süresi", free: "%s / %s MB boş", used: "dolu", logger: "Philips logger",
    clean: "Belleği boşalt", "fl.val": "%s / 500", "ram.title": "Bellek",
    "vol.ext": "TV / soundbar", "np.home": "Ana ekran", "np.label": "Şu an açık", "pad.buttons": "Tuşlar", "pad.touch": "Touchpad", "tpad.hint": "Kaydır: yön · Dokun: OK",
    "favs.lead": "Sırayı değiştirmek için ≡ tutup sürükle.", "favs.more": "Diğer uygulamalar", "favs.none": "Ana sekmede henüz uygulama yok.",
    "ob.skipall": "Geç", "ob.next": "Devam", "ob.later": "Şimdi değil", "ob.done": "Başla",
    "ob.tv.title": "Her şeyin kilidini aç", "ob.tv.text": "TV'deki Odin TV Magic kurulumu adım adım gösterir: geliştirici modu, USB hata ayıklama, sonra <b>İzin ver</b>. Kumandayla yaklaşık bir dakika. İstersen sonra da yapabilirsin.",
    "ob.tv.ok": "TV'ye bağlanıldı", "ob.tv.check": "Tekrar kontrol et",
    "ob.ph.title": "Ambilight ve Philips özellikleri", "ob.ph.text": "Ambilight, görüntü tarzı, parlaklık ve anında tepki veren kumanda için bir kez eşleşmen gerekir. Düğmeye bas: TV 4 haneli bir PIN gösterir, onu buraya yaz.",
    "ob.home.title": "Kumandayı telefonuna ekle", "ob.home.why": "Bir dahaki sefere karekod okutmadan, tek dokunuşla ve uygulama gibi tam ekran açılır.",
    "ob.apps.title": "Uygulamaların", "ob.apps.text": "Ana sekmede görmek istediğin uygulamalara dokun.",
    "ob.done.title": "Hazır", "ob.done.text": "Kumanda, uygulamalar ve ayarlar tek dokunuş uzağında.",
    "src.home": "TV ana ekranı", "src.home.sub": "Uygulamalara dön", "src.home.ok": "Ana ekran",
    "src.tv.sub": "Canlı yayın", "clean.hint": "Arkada bekleyen uygulamaları kapatır. Ekranda açık olan oynamaya devam eder.",
    "src.back": "geri dönmek için \"TV ana ekranı\"",
    "a2hs.title": "Kumandayı telefonuna ekle", "a2hs.why": "Bir dahaki sefere karekod okutmadan, tek dokunuşla ve uygulama gibi tam ekran açılır.",
    "a2hs.ios": "<li>Safari'de <b>Paylaş</b> <span class=\"ios-share\"></span> düğmesine dokun (ekranın altında).</li><li><b>Ana Ekrana Ekle</b>'yi, sonra <b>Ekle</b>'yi seç.</li>",
    "a2hs.android": "<li>Chrome'da <b>⋮</b> menüsüne dokun (sağ üstte).</li><li><b>Ana ekrana ekle</b>'yi (ya da <b>Uygulamayı yükle</b>), sonra <b>Ekle</b>'yi seç.</li>",
    "desk.title": "Bu panel telefonun için yapıldı",
    "desk.text": "Odin TV Magic, <b>telefonundan</b> kullanılan bir TV kumandası ve ayar panelidir. Bilgisayar yalnızca kurulum içindi.",
    "desk.s1": "TV'de <b>Odin TV Magic</b>'i aç.",
    "desk.s2": "TV'deki karekodu <b>telefonunun kamerasıyla</b> okut.",
    "desk.s3": "Telefonda ana ekrana ekle: sonra uygulama gibi açılır.",
    "desk.anyway": "Yine de bu bilgisayardan devam et",
    "a2hs.ok": "Tamam",
    "made.by": "Yapan: %s",
    "upgrade.title": "TV'ni yükselt", "upgrade.sub": "TV'nde olmayan özellikleri ekle.",
    "speed.lead": "Hiç açmadığın ön yüklü uygulamalar arka planda çalışıp bellek harcar.",
    "speed.row": "Kullanılmayan uygulamaları kapat", "speed.sub.n": "%s öneri · %s kapalı",
    "airplay.builtin": "Bu TV'de zaten var", "airplay.add": "Ekle",
    "home.atv": "Android TV ana ekranı (orijinal)", "home.gtv": "Google TV ana ekranı (orijinal)",
    "home.note": "Bazı TV'lerde (ör. Philips) seçiminin geçerli olması için orijinal ana ekran kapatılır. \"Geri getir\" tekrar açar.",
    "speed.title": "TV'yi hızlandır", "speed.sub": "Kullanmadığın ön yüklü uygulamaları kapat",
    "speed.intro": "Kullanmadığın ön yüklü uygulamaları kapat. Hiçbir şey silinmez; hepsini buradan tekrar açabilirsin.",
    "speed.go": "Seçilenleri kapat", "speed.go.n": "%s uygulamayı kapat", "speed.off": "Kapatılanlar", "speed.on": "Aç",
    "speed.none": "Önerilecek bir şey kalmadı.", "speed.confirm": "%s uygulama kapatılsın mı? Sonra kaynakları, sesi ve kumandayı dene. Buradan tekrar açabilirsin.",
    "speed.max": "Bir seferde en fazla %s",
    "g.telemetry": "Takip ve kayıt", "g.unused": "Az kullanılanlar", "g.online": "Philips çevrimiçi servisleri",
    "g.regional": "Ön yüklü bölgesel uygulamalar", "g.preinstalled": "Ön yüklü uygulamalar", "g.launcher": "Eski ana ekran parçaları",
    "look.title": "TV görünümünü değiştir", "look.sub": "Reklamsız, öneri satırları olmayan sade bir ana ekrana geç.",
    "home.install": "Yükle", "launcher.projectivy": "Özelleştirilebilir, hızlı, reklamsız. En popüler seçenek.",
    "launcher.flauncher": "Açık kaynak, sade ve ücretsiz.",
    addons: "Ekstralar", "airplay.desc": "iPhone, iPad ya da Mac ekranını TV'de göster. ~%s MB.",
    "airplay.title": "AirPlay (ekran yansıtma)",
    "airplay.installed": "Yüklü", "airplay.dl": "İndiriliyor, yüzde %s", "airplay.installing": "Kuruluyor…", "airplay.open": "Aç",
    "airplay.starting": "Başlatılıyor…", "airplay.on": "Açık", "airplay.start": "Aç",
    "airplay.off": "Yüklü ama kapalı: iPhone'lar TV'yi göremez.",
    "airplay.how": "<b>iPhone / iPad / Mac:</b> Denetim Merkezi › Ekran Yansıtma › <b>%s</b> (aynı Wi-Fi).",
    apps: "Uygulamalar", sources: "Kaynaklar", tools: "Araçlar", keyboard: "Klavye", screen: "Ekran", sleep: "Uyku", restart: "Yeniden başlat",
    "no.apps": "Uygulama seçilmedi. Ayarlar'dan seç.",
    volume: "Ses", muted: "Sessiz", "pad.hint": "Dokun, kaydır ya da basılı tut", back: "Geri", home: "Ana", menu: "Menü", type: "Klavye ile yaz",
    "philips.title": "Ambilight için bir kez eşleştir",
    "philips.intro": "Ambilight, görüntü tarzı, parlaklık ve anında tepki veren kumanda için TV ile bir kez eşleşmen gerekir: düğmeye bas, TV 4 haneli bir PIN gösterir, onu buraya yaz.",
    "philips.unreachable": "Bu TV'nin Philips arayüzü (JointSpace) yanıt vermiyor; Ambilight ve görüntü yalnızca TV kumandasıyla ayarlanabilir.",
    "amb.need_pair": "Ambilight, görüntü tarzı ve parlaklık TV ile bir kez eşleştikten sonra çalışır: düğmeye bas, TV 4 haneli bir PIN gösterir, onu telefona yaz.",
    "amb.pair_first": "Önce eşleştir: \"TV'de PIN göster\"e bas",
    "diag.open": "Philips tanı bilgisi", "diag.hint": "TV'nin Philips arayüzü hakkında bildirdikleri. Adres, isim ya da anahtar içermez. Hata bildirimine kopyala.",
    "diag.copy": "Kopyala", "diag.copied": "Kopyalandı", "diag.select": "Seçildi: telefonun menüsüyle kopyala",
    "philips.start": "TV'de PIN göster", "philips.enter": "TV ekranındaki PIN'i gir.", "philips.connect": "Bağla",
    "pic.style": "Görüntü tarzı", brightness: "Parlaklık", "b.night": "Gece", "b.evening": "Akşam", "b.day": "Gündüz", "b.max": "Maks",
    on: "Açık", off: "Kapalı", "amb.video": "Video", "amb.music": "Müzik",
    "pic.applying": "%s uygulanıyor… TV'de hızlı menü birkaç saniye görünecek",
    "pic.read": "Tarzları TV'den oku", "pic.none": "Liste henüz okunmadı",
    "pic.no_menu": "Bu TV'de görüntü tarzı TV kumandasıyla değiştirilir; parlaklık buradan çalışır.",
    "scene": "Sinema", "scene.desc": "%s + Ambilight", "scene.busy": "Sinema modu hazırlanıyor…", "scene.ok": "Sinema modu hazır",
    lang: "Dil", favs: "Ana sekmedeki uygulamalar", "styles.show": "Gösterilecek görüntü tarzları",
    party: "Sürpriz modu", "party.name": "Sürpriz",
    "party.desc": "Arkadaşlar için ~45 sn'lik \"TV hacklendi\" gösterisi: alarm, hacker terminali, kendi kendine açılan uygulamalar. Sonunda her şey normale döner.",
    "party.sheet": "TV \"hacklenmiş\" gibi davranır: alarm, hacker terminali, kendi kendine açılan uygulamalar. Sonunda mesajın ekrana gelir. Yaklaşık 45 saniye; istediğin an durdurabilirsin.",
    "party.msg": "Bu TV Odin'in kontrolünde 😎", "party.label": "> SÜRPRİZ_", "party.running": "> ÇALIŞIYOR… DURDUR", "party.stop": "dokun ve her şeyi normale döndür",
    "party.go": "BAŞLAT", "party.started": "Gösteri başladı", "party.stopped": "Her şey normale döndü",
    phones: "Eşleşmiş telefonlar", "this.phone": "bu telefon", remove: "Kaldır", adb: "TV bağlantısı",
    "adb.via": "Durum: %s · bağlantı: %s", about: "Hakkında",
    security: "Paneli yalnızca TV ekranında eşleştirilen telefonlar kullanabilir. İstekler imzalanır; anahtar bu telefondan çıkmaz. Yalnızca ev ağında kullan.",
    unpair: "Bu telefonun eşleşmesini kaldır", "unpair.confirm": "Bu telefonun eşleşmesi kaldırılsın mı? Tekrar eşlemek için TV ekranı gerekir.",
    "tab.home": "Ana", "tab.remote": "Kumanda", "tab.picture": "Görüntü", "tab.settings": "Ayarlar",
    "kb.hint": "TV'de bir yazı kutusu seçiliyken yaz ve gönder. Netflix ya da YouTube aramasında işe yarar.", send: "Gönder",
    "shot.title": "Ekran görüntüsü", "shot.hint": "Kaydetmek için görsele basılı tut. Netflix gibi korumalı içerik açıkken TV görüntü vermez.", refresh: "Yenile",
    "pair.title": "Bu telefonu eşle",
    "pair.how": "TV'de <b>Odin TV Magic</b>'i aç ve karekodu telefonunun kamerasıyla okut ya da orada yazan 6 haneli kodu gir.",
    "pair.go": "Eşle", "pair.short": "6 haneli kodu gir", "paired": "Eşleşti",
    "confirm.reboot": "TV yeniden başlatılsın mı?", "confirm.sleep": "TV uykuya alınsın mı? TV açılana kadar panel çalışmaz.",
    "hours": "%s sa %s dk", "days": "%s gün %s sa", "mins": "%s dk",
    "philips.need": "Önce Philips özelliklerini eşleştir (Görüntü sekmesi)",
    "styles.hint": "Gizlenen tarzlar TV kumandasından kullanılmaya devam eder.",
    "home.title": "Ana ekran", "home.stock": "Orijinal ana ekranı da kapat",
    "home.stock.desc": "Bazı TV'lerde (ör. Philips) Ana tuşunun seçtiğin ekranı açması için gerekir. \"Geri al\" tekrar açar.",
    "home.restore": "Orijinal ana ekranı geri getir", "home.use": "Kullan", "home.current": "Şu anki", "home.stockname": "Orijinal",
    "home.none": "Yalnızca orijinal ana ekran yüklü. Play Store'dan Projectivy Launcher ya da FLauncher kur, sonra buradan seç.",
    "home.gtvnote": "Google TV: orijinal ana ekran açık kalır (kapatmak fabrika ayarı gerektirebilir). Ana tuşu hâlâ Google TV'yi açıyorsa, seçtiğin ana ekranın kendi ayarlarındaki \"override\" seçeneğini aç.",
    "home.confirm": "%s ana ekran yapılsın mı?", "home.restore.confirm": "Orijinal ana ekrana dönülsün mü?",
    "upd.title": "Odin TV Magic %s hazır", "upd.text": "Yüklü sürüm %s. Güncelle'ye dokun: TV indirir, doğrular ve kurar. Eşleşmiş telefonlar eşleşmiş kalır.",
    "upd.go": "Güncelle", "upd.notes": "Yenilikler", "upd.confirm": "Odin TV Magic %s sürümüne güncellensin mi? TV kurarken panel yaklaşık bir dakika kapalı olur.",
    "upd.basic": "Panelden güncellemek için TV'deki kurulum gerekir. Kurulum olmadan: TV'de Downloader'ı aç, odinozlen.com/tv yaz ve eski sürümün üzerine kur (ayarların kalır).",
    "upd.dl": "İndiriliyor, yüzde %s", "upd.verifying": "İndirilen dosya doğrulanıyor…", "upd.installing": "Kuruluyor… panel birazdan kendiliğinden geri gelir.",
    "upd.restarting": "TV güncellemeyi kuruyor. Panel geri gelince sayfa kendiliğinden yenilenir.",
    "upd.failed": "Güncelleme başarısız oldu. Hiçbir şey değişmedi.", "upd.done": "%s sürümüne güncellendi",
    "upd.section": "Güncellemeler", "upd.auto": "Otomatik güncelleme kontrolü",
    "upd.check": "Şimdi denetle", "upd.off": "Güncelleme denetimi kapalı", "upd.never": "Henüz denetlenmedi",
    "upd.current": "Güncel (%s) · son denetim %s", "upd.avail": "%s hazır",
  },
};
let lang = "en";
try { lang = localStorage.getItem("odintv-lang") || ""; } catch (e) {}
if (!I18N[lang]) lang = /^tr/i.test(navigator.language || "") ? "tr" : "en";
function t(key, ...args) {
  let s = (I18N[lang] && I18N[lang][key]) || I18N.en[key] || key;
  args.forEach(a => { s = s.replace("%s", a); });
  return s;
}
function applyI18n() {
  document.documentElement.lang = lang;
  document.querySelectorAll("[data-i18n]").forEach(el => { el.textContent = t(el.dataset.i18n); });
  // Only our own constant strings go through innerHTML (they contain <b>).
  document.querySelectorAll("[data-i18n-html]").forEach(el => { el.innerHTML = t(el.dataset.i18nHtml); });
  $("surMsg").placeholder = t("party.msg");
  $("surGo").textContent = t("party.go");
  document.querySelectorAll("[data-lang]").forEach(b => b.classList.toggle("on", b.dataset.lang === lang));
  if (lastStatus) renderStatus(lastStatus);
  if (lastPicture) renderPicture(lastPicture);
}
function setLang(l) {
  lang = l;
  try { localStorage.setItem("odintv-lang", l); } catch (e) {}
  applyI18n();
}

/* ---------------- SHA-256 / HMAC (crypto.subtle is unavailable on plain http) ---------------- */
const K256 = new Uint32Array([
  0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,
  0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
  0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,
  0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
  0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,
  0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2]);
function sha256(bytes) {
  const l = bytes.length, bitLen = l * 8;
  const n = ((l + 9 + 63) >> 6) << 6;
  const m = new Uint8Array(n);
  m.set(bytes); m[l] = 0x80;
  const dv = new DataView(m.buffer);
  dv.setUint32(n - 8, Math.floor(bitLen / 0x100000000)); dv.setUint32(n - 4, bitLen >>> 0);
  const h = new Uint32Array([0x6a09e667,0xbb67ae85,0x3c6ef372,0xa54ff53a,0x510e527f,0x9b05688c,0x1f83d9ab,0x5be0cd19]);
  const w = new Uint32Array(64);
  const rotr = (x, k) => (x >>> k) | (x << (32 - k));
  for (let off = 0; off < n; off += 64) {
    for (let i = 0; i < 16; i++) w[i] = dv.getUint32(off + i * 4);
    for (let i = 16; i < 64; i++) {
      const s0 = rotr(w[i-15], 7) ^ rotr(w[i-15], 18) ^ (w[i-15] >>> 3);
      const s1 = rotr(w[i-2], 17) ^ rotr(w[i-2], 19) ^ (w[i-2] >>> 10);
      w[i] = (w[i-16] + s0 + w[i-7] + s1) >>> 0;
    }
    let [a, b, c, d, e, f, g, hh] = h;
    for (let i = 0; i < 64; i++) {
      const S1 = rotr(e, 6) ^ rotr(e, 11) ^ rotr(e, 25);
      const t1 = (hh + S1 + ((e & f) ^ (~e & g)) + K256[i] + w[i]) >>> 0;
      const S0 = rotr(a, 2) ^ rotr(a, 13) ^ rotr(a, 22);
      const t2 = (S0 + ((a & b) ^ (a & c) ^ (b & c))) >>> 0;
      hh = g; g = f; f = e; e = (d + t1) >>> 0; d = c; c = b; b = a; a = (t1 + t2) >>> 0;
    }
    h[0] += a; h[1] += b; h[2] += c; h[3] += d; h[4] += e; h[5] += f; h[6] += g; h[7] += hh;
  }
  const out = new Uint8Array(32), odv = new DataView(out.buffer);
  for (let i = 0; i < 8; i++) odv.setUint32(i * 4, h[i]);
  return out;
}
function hmac256(key, msg) {
  if (key.length > 64) key = sha256(key);
  const k = new Uint8Array(64); k.set(key);
  const ip = new Uint8Array(64 + msg.length), op = new Uint8Array(64 + 32);
  for (let i = 0; i < 64; i++) { ip[i] = k[i] ^ 0x36; op[i] = k[i] ^ 0x5c; }
  ip.set(msg, 64);
  op.set(sha256(ip), 64);
  return sha256(op);
}
const enc = new TextEncoder();
const hex = b => Array.from(b, x => x.toString(16).padStart(2, "0")).join("");
function b64urlDecode(s) {
  s = s.replace(/-/g, "+").replace(/_/g, "/");
  while (s.length % 4) s += "=";
  const bin = atob(s), out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}
function randomHex(n) { const b = new Uint8Array(n); crypto.getRandomValues(b); return hex(b); }

/* ---------------- credentials & API ---------------- */
const $ = id => document.getElementById(id);
let cred = null, keyBytes = null, clockOffset = 0;
try { cred = JSON.parse(localStorage.getItem("odintv-cred") || "null"); } catch (e) {}
if (cred && cred.id && cred.key) keyBytes = b64urlDecode(cred.key); else cred = null;

class ApiError extends Error { constructor(msg, status, data) { super(msg); this.status = status; this.data = data || {}; } }

async function syncClock() {
  const t0 = Date.now();
  const r = await fetch("/api/hello", { cache: "no-store" });
  const d = await r.json();
  clockOffset = d.time - Math.round((t0 + Date.now()) / 2);
  return d;
}

async function rawApi(path, method, body, retried) {
  method = method || "GET";
  const bodyStr = body === undefined ? "" : JSON.stringify(body);
  const headers = { "X-Lang": lang };
  if (cred) {
    const ts = String(Date.now() + clockOffset), nonce = randomHex(12);
    const msg = method + "\n" + path + "\n" + ts + "\n" + nonce + "\n" + hex(sha256(enc.encode(bodyStr)));
    headers["X-Auth"] = cred.id + "." + ts + "." + nonce + "." + hex(hmac256(keyBytes, enc.encode(msg)));
  }
  if (bodyStr) headers["Content-Type"] = "application/json";
  const r = await fetch(path, { method, headers, body: bodyStr || undefined, cache: "no-store" });
  if (r.status === 401) {
    let d = {};
    try { d = await r.json(); } catch (e) {}
    if (d.reason === "clock" && !retried) {
      clockOffset = d.time - Date.now();
      return rawApi(path, method, body, true);
    }
    if (d.reason === "pair") { forgetCred(); showPairing(d.msg); }
    throw new ApiError(d.msg || "401", 401, d);
  }
  return r;
}
async function api(path, method, body) {
  const r = await rawApi(path, method, body);
  let d = {};
  try { d = await r.json(); } catch (e) {}
  if (!r.ok) throw new ApiError(d.msg || ("HTTP " + r.status), r.status, d);
  return d;
}
async function apiBlob(path) {
  const r = await rawApi(path, "GET");
  if (!r.ok) { let m = "HTTP " + r.status; try { m = (await r.json()).msg || m; } catch (e) {} throw new ApiError(m, r.status); }
  return r.blob();
}
function forgetCred() {
  cred = null; keyBytes = null;
  try { localStorage.removeItem("odintv-cred"); } catch (e) {}
}

/* ---------------- UI helpers ---------------- */
let toastTimer;
function toast(msg, err) {
  const el = $("toast");
  el.textContent = msg;
  el.classList.toggle("err", !!err);
  el.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.remove("show"), err ? 4500 : 3000);
}
$("toast").addEventListener("click", () => $("toast").classList.remove("show"));
async function busy(el, fn) {
  el.classList.add("busy");
  try { return await fn(); }
  catch (e) { if (!(e instanceof ApiError && e.status === 401)) toast(e.message || "Error", true); }
  finally { el.classList.remove("busy"); }
}
let openSheet = null;
function showSheet(id) { closeSheet(); openSheet = $(id); openSheet.classList.add("show"); $("scrim").classList.add("show"); }
function closeSheet() { if (openSheet) openSheet.classList.remove("show"); $("scrim").classList.remove("show"); openSheet = null; }
$("scrim").addEventListener("click", closeSheet);
// Every sheet gets a close button, and can be dragged down by its top area.
document.querySelectorAll(".sheet").forEach(sh => {
  const x = el("button", "sheet-x");
  x.setAttribute("aria-label", "close");
  x.innerHTML = '<svg class="i" viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg>';
  x.addEventListener("click", closeSheet);
  sh.insertBefore(x, sh.firstChild);
  let y0 = null, dy = 0;
  sh.addEventListener("touchstart", e => { y0 = sh.scrollTop <= 0 ? e.touches[0].clientY : null; dy = 0; }, { passive: true });
  sh.addEventListener("touchmove", e => {
    if (y0 === null) return;
    dy = e.touches[0].clientY - y0;
    if (dy > 0) { sh.style.transition = "none"; sh.style.transform = "translateY(" + dy + "px)"; }
  }, { passive: true });
  sh.addEventListener("touchend", () => {
    sh.style.transition = ""; sh.style.transform = "";
    if (y0 !== null && dy > 90) closeSheet();
    y0 = null;
  });
});
function el(tag, cls, text) { const e = document.createElement(tag); if (cls) e.className = cls; if (text !== undefined) e.textContent = text; return e; }

/* ---------------- pairing screen ---------------- */
function showPairing(msg) {
  $("pairScreen").hidden = false;
  $("pairErr").textContent = msg || "";
}
function deviceName() {
  const ua = navigator.userAgent;
  const dev = /iPhone/.test(ua) ? "iPhone" : /iPad/.test(ua) ? "iPad" : /Android/.test(ua) ? "Android" : /Mac/.test(ua) ? "Mac" : /Windows/.test(ua) ? "Windows" : "Browser";
  const br = /CriOS|Chrome/.test(ua) ? "Chrome" : /FxiOS|Firefox/.test(ua) ? "Firefox" : /Safari/.test(ua) ? "Safari" : "";
  return (dev + " " + br).trim();
}
async function pairWith(code) {
  code = (code || "").replace(/\D/g, "");
  if (code.length !== 6) { $("pairErr").textContent = t("pair.short"); return; }
  await busy($("pairGo"), async () => {
    const r = await fetch("/api/pair", { method: "POST", headers: { "Content-Type": "application/json", "X-Lang": lang },
      body: JSON.stringify({ code, name: deviceName() }), cache: "no-store" });
    const d = await r.json().catch(() => ({}));
    if (!r.ok) { $("pairErr").textContent = d.msg || ("HTTP " + r.status); return; }
    cred = { id: d.id, key: d.key };
    keyBytes = b64urlDecode(d.key);
    try { localStorage.setItem("odintv-cred", JSON.stringify(cred)); } catch (e) {}
    $("pairScreen").hidden = true;
    toast(t("paired"));
    try { localStorage.removeItem("odintv-onboard"); } catch (e) {}
    start();
  });
}
$("pairGo").addEventListener("click", () => pairWith($("pairCode").value));
$("pairCode").addEventListener("input", e => { if (e.target.value.replace(/\D/g, "").length === 6) pairWith(e.target.value); });
document.querySelectorAll("[data-lang]").forEach(b => b.addEventListener("click", () => setLang(b.dataset.lang)));

/* ---------------- tabs ---------------- */
function showTab(name) {
  if (name === "picture" && $("tabPicture").hidden) name = "home";
  document.querySelectorAll(".page").forEach(p => p.classList.toggle("show", p.dataset.page === name));
  document.querySelectorAll("[data-tab]").forEach(b => b.classList.toggle("on", b.dataset.tab === name));
  try { localStorage.setItem("odintv-tab", name); } catch (e) {}
  window.scrollTo(0, 0);
  if (name === "remote") loadVolume();
  if (name === "picture") loadPicture();
  if (name === "settings") loadSettings();
  updateNp();
}
document.querySelectorAll("[data-tab]").forEach(b => b.addEventListener("click", () => showTab(b.dataset.tab)));
function currentTab() { const b = document.querySelector("[data-tab].on"); return b ? b.dataset.tab : "home"; }

/* ---------------- status ---------------- */
let lastStatus = null, appLabels = {};
function setOnline(on, label) {
  $("dot").className = "dot " + (on ? "on" : "off");
  $("state").textContent = label || t(on ? "online" : "offline");
}
function fmtUptime(h) {
  const total = Math.round(h * 60), d = Math.floor(total / 1440), hh = Math.floor(total % 1440 / 60), mm = total % 60;
  return d > 0 ? t("days", d, hh) : hh > 0 ? t("hours", hh, mm) : t("mins", mm);
}
function renderStatus(s) {
  lastStatus = s;
  const adbOk = s.adb && s.adb.ok;
  setOnline(true);
  $("now").textContent = "";
  if (adbOk || s.foreground) {
    $("now").append(t("now"));
    const src = !s.foreground_home && (s.sources || []).find(x => x.id === s.active_input);
    const name = s.foreground_home ? t("home_screen") : s.foreground === "com.odin.tvpanel" ? "Odin TV Magic"
      : src ? src.name : (appLabels[s.foreground] || s.foreground_label || s.foreground || "—");
    $("now").appendChild(el("b", "", name));
  } else {
    $("now").textContent = s.model || "";
  }
  $("setup").hidden = adbOk;
  const pi0 = s.philips_info || {};
  $("setupBasic").innerHTML = t(s.philips && pi0.paired ? "setup.basic.ph" : "setup.basic");  // our own constant strings
  applyCaps(s.caps || { adb: !!adbOk });
  // Pairing with a code needs Android 12+ on the TV; older TVs only have option A.
  $("setupB").hidden = typeof s.sdk === "number" && s.sdk < 31;
  // The setup card explains what to do; the raw state only helps while the TV waits for "Allow".
  const k = s.adb && s.adb.key;
  $("setupState").textContent = /approv|rejected/.test(k || "") ? s.adb.state : "";
  $("setupState").hidden = !$("setupState").textContent;
  const usedPct = Math.round(100 * (s.total_mb - s.free_mb) / Math.max(1, s.total_mb));
  $("ramPct").textContent = lang === "tr" ? "%" + usedPct : usedPct + "%";
  $("ramRing").style.strokeDashoffset = String(201 * (1 - usedPct / 100));
  $("ramRing").style.stroke = usedPct > 88 ? "var(--bad)" : usedPct > 78 ? "var(--warn)" : "var(--ok)";
  $("ramSub").textContent = t("free", s.free_mb, s.total_mb);
  $("up").textContent = fmtUptime(s.uptime_h);

  const pi = s.philips_info || {};
  const hasLogger = typeof pi.fl_threads === "number";
  $("flStat").hidden = !hasLogger;
  if (hasLogger) $("fl").textContent = t("fl.val", pi.fl_threads);
  const jsDown = pi.reachable === false;
  $("philipsHomeCard").hidden = !(s.philips && !pi.paired && !jsDown);
  // Philips tab
  $("tabPicture").hidden = !s.philips;
  // Ambilight shows before pairing too, locked, with the pairing button in it: people did not
  // connect a "Philips features" card with Ambilight. Hidden only when the TV says it has none.
  const ambShown = !!s.philips && !jsDown && pi.has_ambilight !== false;
  $("philipsPairCard").hidden = !s.philips || !!pi.paired || ambShown;
  $("philipsNoJs").hidden = !jsDown;
  $("philipsPair").hidden = jsDown;
  $("picCard").hidden = !pi.paired;
  $("ambTitle").hidden = $("ambCard").hidden = !ambShown;
  $("ambLock").hidden = !!pi.paired;
  $("ambSeg").classList.toggle("needpair", !pi.paired);
  $("ambSws").classList.toggle("needpair", !pi.paired);
  $("philipsDiag").hidden = !s.philips;
  $("ambOn").classList.toggle("on", pi.ambilight === "On");
  $("ambOff").classList.toggle("on", pi.ambilight === "Off");
  $("styleSettings").hidden = !s.philips;

  renderSources(adbOk ? (s.sources || []) : [], s.active_input, s.foreground_home);
  updateNp();
  renderParty(s.party, s.surprise);
  $("adbInfo").textContent = s.adb ? t("adb.via", s.adb.state, s.adb.transport || "—") : "";
  $("about").textContent = "Odin TV Magic " + s.version + " · " + (s.model || "");
  renderUpdate(s.update);
  renderMadeBy();
}
async function refresh() {
  if (!cred) return;
  try { renderStatus(await api("/api/status")); }
  catch (e) {
    if (e instanceof ApiError && e.status === 401) return;
    setOnline(false); $("now").textContent = t("tv.off");
  }
}

/* ---------------- basic mode: dim what needs the TV setup ---------------- */
let caps = {};
function applyCaps(c) {
  caps = c;
  // Without the remote keys, play/pause still works through Android's media keys.
  $("basicMedia").hidden = !(c.nav === false && c.media);
  document.querySelectorAll("[data-cap]").forEach(e => {
    const off = c[e.dataset.cap] === false || (c[e.dataset.cap] === undefined && !c.adb);
    e.classList.toggle("locked", off);
    e.setAttribute("aria-disabled", off ? "true" : "false");
  });
}
// A locked control explains itself instead of failing.
["pointerdown", "click"].forEach(type => document.addEventListener(type, e => {
  const l = e.target.closest && e.target.closest(".locked");
  if (!l) return;
  e.preventDefault(); e.stopPropagation();
  if (type === "click") toast(t("needs.setup"), true);
}, true));

/* ---------------- playing now bar ---------------- */
function updateNp() {
  const s = lastStatus, bar = $("npBar");
  const tab = currentTab();
  // A floating mini player above the tab bar, only while an app or input is on screen. It is
  // out of the page flow, so showing or hiding it never moves anything under the user's finger.
  const atHome = !s || s.foreground_home || !s.foreground || s.foreground === "com.odin.tvpanel";
  const show = !!(s && s.foreground) && !atHome && (tab === "home" || tab === "remote");
  bar.hidden = !show;
  document.body.classList.toggle("np-on", show);
  if (!show) return;
  const src = atHome ? null : (s.sources || []).find(x => x.id === s.active_input);
  $("npName").textContent = atHome ? t("np.home") : src ? src.name : (appLabels[s.foreground] || s.foreground_label || s.foreground);
  // On the home screen the bar's own Home button would do nothing; its room goes to the name.
  document.querySelector('#npBar [data-r="home"]').hidden = atHome;
  const img = $("npIcon"), glyph = $("npGlyph");
  glyph.hidden = !(atHome || src);
  if (atHome || src) {
    img.removeAttribute("src"); img.style.display = "none"; img.dataset.pkg = "";
    glyph.innerHTML = atHome ? HOME_ICON : src.tuner ? TV_ICON : SRC_ICON;
  } else if (img.dataset.pkg !== s.foreground) {
    img.style.display = "";
    img.dataset.pkg = s.foreground; img.style.visibility = ""; img.removeAttribute("src");
    iconFor(s.foreground, img);
  }
}

/* ---------------- touchpad ---------------- */
(function touchpad() {
  const tp = $("tpad"), dot = $("tdot");
  let mode = "buttons";
  try { mode = localStorage.getItem("odintv-pad") || "buttons"; } catch (e) {}
  function setMode(m) {
    mode = m;
    try { localStorage.setItem("odintv-pad", m); } catch (e) {}
    $("tpad").hidden = m !== "touch";
    $("pad").hidden = m === "touch";
    document.querySelector(".pad-hint").hidden = m === "touch";
    document.querySelectorAll("[data-pad]").forEach(b => b.classList.toggle("on", b.dataset.pad === m));
  }
  document.querySelectorAll("[data-pad]").forEach(b => b.addEventListener("click", () => setMode(b.dataset.pad)));
  setMode(mode);
  const STEP = 42;
  let o = null, start = null, moved = false;
  tp.addEventListener("pointerdown", e => {
    try { tp.setPointerCapture(e.pointerId); } catch (x) {}
    const r = tp.getBoundingClientRect();
    o = { x: e.clientX, y: e.clientY }; start = { x: e.clientX, y: e.clientY, t: Date.now() }; moved = false;
    dot.style.left = (e.clientX - r.left) + "px"; dot.style.top = (e.clientY - r.top) + "px"; dot.classList.add("on");
  });
  tp.addEventListener("pointermove", e => {
    if (!o) return;
    const r = tp.getBoundingClientRect();
    dot.style.left = (e.clientX - r.left) + "px"; dot.style.top = (e.clientY - r.top) + "px";
    const dx = e.clientX - o.x, dy = e.clientY - o.y;
    if (Math.abs(dx) >= STEP || Math.abs(dy) >= STEP) {
      // every STEP pixels of travel = one key press, like a trackpad
      sendKey(Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? "right" : "left") : (dy > 0 ? "down" : "up"));
      o = { x: e.clientX, y: e.clientY };
      moved = true;
    }
  });
  function end(e) {
    if (!start) return;
    dot.classList.remove("on");
    const far = Math.hypot(e.clientX - start.x, e.clientY - start.y);
    if (!moved && far < 12 && Date.now() - start.t < 500) sendKey("ok");
    o = start = null;
  }
  tp.addEventListener("pointerup", end);
  tp.addEventListener("pointercancel", () => { dot.classList.remove("on"); o = start = null; });
})();

/* ---------------- onboarding ---------------- */
let obSteps = [], obIndex = 0, obFavs = null;
function onboardingDone() {
  try { return localStorage.getItem("odintv-onboard") === "1"; } catch (e) { return true; }
}
function finishOnboarding() {
  try { localStorage.setItem("odintv-onboard", "1"); localStorage.setItem("odintv-a2hs", "1"); } catch (e) {}
  $("onboard").hidden = true;
  if (obFavs) { saveSettings({ favorites: obFavs }, true).then(loadApps); obFavs = null; }
  refresh();
}
async function startOnboarding() {
  if (onboardingDone()) return;
  const s = lastStatus || {};
  const standalone = window.navigator.standalone || window.matchMedia("(display-mode: standalone)").matches;
  obSteps = [];
  // Philips first: one PIN unlocks most features right away. The ADB setup happens on the TV.
  if (s.philips && !(s.philips_info && s.philips_info.paired)) obSteps.push("philips");
  if (!(s.adb && s.adb.ok)) obSteps.push("tv");
  if (!standalone && !isComputer()) obSteps.push("home");
  obSteps.push("apps", "done");
  obIndex = 0;
  if (!allApps.length) await loadApps();
  obFavs = allApps.filter(a => a.fav).map(a => a.pkg);
  $("onboard").hidden = false;
  renderOb();
}
function obIcon(cls, svg) { const d = el("div", "big-ic " + cls); d.innerHTML = svg; return d; }
function renderOb() {
  const step = obSteps[obIndex], body = $("obBody");
  body.innerHTML = "";
  $("obDots").innerHTML = obSteps.map((_, i) => '<i class="' + (i <= obIndex ? "on" : "") + '"></i>').join("");
  $("obNext").textContent = step === "done" ? t("ob.done") : t("ob.next");
  $("obLater").hidden = step === "done" || step === "apps";
  $("obSkip").hidden = step === "done";
  const h = (k) => el("h3", "", t(k));
  const p = (k) => { const d = el("div", "hint"); d.innerHTML = t(k); return d; };  // our own constant strings
  if (step === "tv") {
    body.append(obIcon("c-orange", '<svg class="i" viewBox="0 0 24 24"><rect x="2.5" y="4" width="19" height="13" rx="2.5"/><path d="M8 21h8M12 17v4"/></svg>'), h("ob.tv.title"), p("ob.tv.text"));
    const st = el("div", "hint", (lastStatus && lastStatus.adb) ? lastStatus.adb.state : "");
    const b = el("button", "btn tint", t("ob.tv.check"));
    b.style.margin = "0 auto";
    b.addEventListener("click", () => busy(b, async () => {
      await retryAdb();
      if (lastStatus && lastStatus.adb && lastStatus.adb.ok) { st.textContent = "✓ " + t("ob.tv.ok"); }
      else st.textContent = lastStatus && lastStatus.adb ? lastStatus.adb.state : "";
    }));
    body.append(st, b);
  } else if (step === "philips") {
    body.append(obIcon("c-blue", '<svg class="i" viewBox="0 0 24 24"><circle cx="12" cy="12" r="4"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3M5 5l2 2M17 17l2 2M5 19l2-2M17 7l2-2"/></svg>'), h("ob.ph.title"), p("ob.ph.text"));
    const start = el("button", "btn fill", t("philips.start"));
    start.style.margin = "0 auto";
    const row = el("div", "codeRow"); row.hidden = true;
    const pin = el("input", "field code"); pin.inputMode = "numeric"; pin.maxLength = 6;
    const ok = el("button", "btn fill", t("philips.connect"));
    row.append(pin, ok);
    start.addEventListener("click", () => busy(start, async () => { await api("/api/philips/pair/start", "POST"); row.hidden = false; start.hidden = true; pin.focus(); }));
    ok.addEventListener("click", () => busy(ok, async () => {
      const v = pin.value.replace(/\D/g, "");
      if (v.length < 4) return;
      toast((await api("/api/philips/pair/finish", "POST", { pin: v })).msg);
      await refresh();
      obIndex++; renderOb();
    }));
    body.append(start, row);
  } else if (step === "home") {
    const ic = el("div", "a2hs-icon"); const img = el("img"); img.src = "/icon-magic.png"; ic.appendChild(img);
    const ol = el("ol", "a2hs-steps"); ol.innerHTML = t(/iPhone|iPad|iPod/.test(navigator.userAgent) ? "a2hs.ios" : "a2hs.android");  // our own constant strings
    body.append(ic, h("ob.home.title"), p("ob.home.why"), ol);
  } else if (step === "apps") {
    body.append(h("ob.apps.title"), p("ob.apps.text"));
    const grid = el("div", "ob-apps");
    allApps.filter(a => a.tv || a.fav).forEach(a => {
      const b = el("button", "app" + (obFavs.includes(a.pkg) ? " on" : ""));
      const ic = el("span", "ic"), img = el("img"); img.alt = ""; ic.appendChild(img); iconFor(a.pkg, img);
      b.append(ic, el("span", "lbl", a.label));
      b.addEventListener("click", () => {
        const i = obFavs.indexOf(a.pkg);
        if (i >= 0) obFavs.splice(i, 1); else if (obFavs.length < 16) obFavs.push(a.pkg);
        b.classList.toggle("on", obFavs.includes(a.pkg));
      });
      grid.appendChild(b);
    });
    body.appendChild(grid);
  } else {
    body.append(obIcon("c-green", '<svg class="i" viewBox="0 0 24 24"><path d="M5 12.5l4.5 4.5L19 7"/></svg>'), h("ob.done.title"), p("ob.done.text"));
  }
}
$("obNext").addEventListener("click", () => { if (obIndex >= obSteps.length - 1) finishOnboarding(); else { obIndex++; renderOb(); } });
$("obLater").addEventListener("click", () => { obIndex++; renderOb(); });
$("obSkip").addEventListener("click", finishOnboarding);

/* ---------------- setup (ADB) ---------------- */
async function adbPair(inputId, btn) {
  const code = $(inputId).value.replace(/\D/g, "");
  if (code.length !== 6) { toast(t("pair.short"), true); return; }
  await busy(btn, async () => {
    const r = await api("/api/adb/pair", "POST", { code });
    toast(r.msg);
    $(inputId).value = "";
    setTimeout(refresh, 1500);
  });
}
$("adbPair").addEventListener("click", e => adbPair("adbCode", e.currentTarget));
$("adbPair2").addEventListener("click", e => adbPair("adbCode2", e.currentTarget));
// "Check again" may show the TV's prompt again (polling never does once it was dismissed).
async function retryAdb() { try { await api("/api/adb/retry", "POST"); } catch (e) {} await refresh(); }
$("setupRetry").addEventListener("click", e => busy(e.currentTarget, retryAdb));

/* ---------------- actions ---------------- */
document.querySelectorAll("[data-a]").forEach(b => b.addEventListener("click", async () => {
  const a = b.dataset.a;
  if (a === "reboot" && !confirm(t("confirm.reboot"))) return;
  if (a === "sleep" && !confirm(t("confirm.sleep"))) return;
  if (a === "clean") { cleaning = true; lockSources(); }
  await busy(b, async () => {
    const r = await api("/api/action/" + a, "POST");
    toast(r.msg);
    if (a === "clean") { $("cleanSub").textContent = r.msg; $("cleanSub").classList.remove("hint"); loadDebloatCount(); }
    if (a === "reboot" || a === "sleep") setOnline(false, t(a === "reboot" ? "restarting" : "sleeping"));
    else setTimeout(refresh, 1200);
  });
  // keep source buttons paused a moment longer: the page settles after the result shows
  if (a === "clean") setTimeout(() => { cleaning = false; lockSources(); }, 1500);
}));

/* ---------------- apps ---------------- */
const iconCache = {};
async function iconFor(pkg, img) {
  try {
    if (!iconCache[pkg]) iconCache[pkg] = URL.createObjectURL(await apiBlob("/api/appicon/" + pkg));
    img.src = iconCache[pkg];
  } catch (e) {}
}
let allApps = [];
async function loadApps() {
  try {
    allApps = (await api("/api/apps")).apps || [];
    allApps.forEach(a => { appLabels[a.pkg] = a.label; });
    renderApps();
  } catch (e) {}
}
function renderApps() {
  const box = $("apps");
  box.innerHTML = "";
  const favs = allApps.filter(a => a.fav);
  if (!favs.length) { box.appendChild(el("div", "empty", t("no.apps"))); return; }
  favs.forEach(a => {
    const b = el("button", "app");
    const ic = el("span", "ic"), img = el("img");
    img.alt = "";
    ic.appendChild(img);
    b.append(ic, el("span", "lbl", a.label));
    iconFor(a.pkg, img);
    b.addEventListener("click", () => busy(b, async () => {
      const r = await api("/api/launch/" + a.pkg, "POST");
      toast(r.msg);
      setTimeout(refresh, 1500);
    }));
    box.appendChild(b);
  });
}

/* ---------------- sources ---------------- */
const SRC_ICON = '<svg class="i" viewBox="0 0 24 24"><rect x="3" y="6" width="18" height="10" rx="2"/><path d="M7 16v2M17 16v2M7 10h.01M10 10h4"/></svg>';
const TV_ICON = '<svg class="i" viewBox="0 0 24 24"><rect x="2.5" y="7" width="19" height="13" rx="2.5"/><path d="M8 3l4 4 4-4"/></svg>';
const HOME_ICON = '<svg class="i" viewBox="0 0 24 24"><path d="M3 10.5L12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z"/></svg>';
let cleaning = false, srcSig = "";
function lockSources() {
  document.querySelectorAll("#sources .src").forEach(b => b.classList.toggle("wait", cleaning));
}
// A tap only counts on a tile that has been on screen for a moment: protects against the page
// shifting under a finger (a tap meant for something else must never switch the TV's input).
function srcTap(b, fn) {
  b.addEventListener("click", () => {
    if (cleaning || performance.now() - (b._born || 0) < 700) return;
    busy(b, fn);
  });
}
function renderSources(list, activeId, atHome) {
  const active = list.filter(s => s.connected);
  $("sourcesBox").hidden = !active.length;
  const box = $("sources");
  // Rebuild only when the list itself changes; otherwise just move the highlight.
  const sig = lang + "|" + active.map(s => s.id + "=" + s.name).join(",");
  if (sig === srcSig && box.children.length) {
    [...box.children].forEach(b => b.classList.toggle("on", b._id ? b._id === activeId : !!atHome));
    lockSources();
    return;
  }
  srcSig = sig;
  box.innerHTML = "";
  const born = performance.now();
  if (active.length) {
    // way back from an HDMI input to the TV's own home screen
    const h = el("button", "src");
    h._born = born;
    const ic = el("span", "ic home");
    ic.innerHTML = HOME_ICON;
    const tx = el("span", "t");
    tx.append(el("b", "", t("src.home")), el("small", "", t("src.home.sub")));
    h.append(ic, tx);
    h.classList.toggle("on", !!atHome);
    srcTap(h, async () => { await api("/api/remote/home", "POST"); toast(t("src.home.ok")); setTimeout(refresh, 1200); });
    box.appendChild(h);
  }
  active.forEach(src => {
    const b = el("button", "src");
    b._born = born; b._id = src.id;
    const ic = el("span", "ic" + (src.tuner ? " tv" : ""));
    ic.innerHTML = src.tuner ? TV_ICON : SRC_ICON;
    const tx = el("span", "t");
    tx.append(el("b", "", src.name));
    if (src.tuner) tx.append(el("small", "", t("src.tv.sub")));
    else if (src.name !== src.label) tx.append(el("small", "", src.label));
    b.append(ic, tx);
    b.classList.toggle("on", src.id === activeId);
    srcTap(b, async () => {
      const r = await api("/api/source", "POST", { id: src.id });
      toast(r.msg + " · " + t("src.back"));
      setTimeout(refresh, 1500);
    });
    box.appendChild(b);
  });
}

/* ---------------- remote ---------------- */
let keyChain = Promise.resolve();
function sendKey(k) {
  // Keys go one after another so fast taps arrive in order.
  keyChain = keyChain.then(() => api("/api/remote/" + k, "POST")).catch(e => { if (!(e instanceof ApiError && e.status === 401)) toast(e.message, true); });
  return keyChain;
}
(function pad() {
  const pad = $("pad"), glow = $("padGlow"), ok = $("okb");
  const ANG = { up: 0, right: 90, down: 180, left: 270 };
  let start = null, dir = null, holdT = null, repT = null, repeated = false;
  function flash(d) {
    const e = d === "ok" ? ok : pad.querySelector('.ar[data-dir="' + d + '"]');
    if (d !== "ok" && glow) { glow.style.setProperty("--a", ANG[d] + "deg"); glow.classList.add("hit"); }
    e.classList.add("hit");
    setTimeout(() => { e.classList.remove("hit"); if (glow) glow.classList.remove("hit"); }, 160);
  }
  function zone(x, y) {
    const r = pad.getBoundingClientRect();
    const dx = x - (r.left + r.width / 2), dy = y - (r.top + r.height / 2);
    if (Math.hypot(dx, dy) < r.width * 0.2) return "ok";
    return Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? "right" : "left") : (dy > 0 ? "down" : "up");
  }
  function stop() { clearTimeout(holdT); clearInterval(repT); holdT = repT = null; }
  pad.addEventListener("pointerdown", e => {
    try { pad.setPointerCapture(e.pointerId); } catch (x) {}
    start = { x: e.clientX, y: e.clientY };
    dir = zone(e.clientX, e.clientY);
    repeated = false;
    if (dir !== "ok") holdT = setTimeout(() => { repeated = true; repT = setInterval(() => { flash(dir); sendKey(dir); }, 160); }, 420);
  });
  pad.addEventListener("pointermove", e => { if (start && Math.hypot(e.clientX - start.x, e.clientY - start.y) > 24 && !repeated) stop(); });
  pad.addEventListener("pointerup", e => {
    if (!start) return;
    stop();
    const dx = e.clientX - start.x, dy = e.clientY - start.y;
    let k = dir;
    if (Math.hypot(dx, dy) > 28) k = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? "right" : "left") : (dy > 0 ? "down" : "up");
    else if (repeated) k = null;
    start = null;
    if (k) { flash(k); sendKey(k); }
  });
  pad.addEventListener("pointercancel", () => { stop(); start = null; });
})();
document.querySelectorAll("[data-r]").forEach(b => b.addEventListener("click", () => sendKey(b.dataset.r)));

/* ---------------- volume ---------------- */
let volState = null, volTimer = null;
function paintVol() {
  const v = $("vol");
  v.style.setProperty("--p", (100 * (v.value - v.min) / Math.max(1, v.max - v.min)) + "%");
  const muted = volState && volState.muted;
  v.classList.toggle("muted", !!muted);
  $("mute").classList.toggle("on", !!muted);
  $("volVal").textContent = muted ? t("muted") : v.value;
}
// "steps" mode: the TV does not report a usable level (HDMI stick, soundbar over ARC, unknown
// format). Then only − / + / mute are shown and they send the remote's volume keys.
function volSteps(on) {
  $("vol").hidden = on;
  $("volCard").classList.toggle("steps", on);
}
async function loadVolume() {
  try {
    volState = await api("/api/volume");
    volSteps(!!volState.fixed);
    if (volState.fixed) { $("volVal").textContent = volState.muted ? t("muted") : t("vol.ext"); return; }
    if (!$("vol").matches(":active")) { $("vol").min = volState.min; $("vol").max = volState.max; $("vol").value = volState.current; }
    paintVol();
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) return;
    volState = null; volSteps(true); $("volVal").textContent = t("vol.ext");
  }
}
function sendVol(body) {
  return api("/api/volume", "POST", body).then(r => { toast(r.msg); return loadVolume(); })
    .catch(e => { if (!(e instanceof ApiError && e.status === 401)) toast(e.message, true); });
}
$("vol").addEventListener("input", () => { if (volState) volState.muted = false; paintVol(); });
$("vol").addEventListener("change", () => sendVol({ value: Number($("vol").value), muted: false }));
document.querySelectorAll("[data-vol]").forEach(b => b.addEventListener("click", () => {
  if (!volState || volState.fixed) { sendVol({ step: Number(b.dataset.vol) }); return; }
  const v = $("vol");
  v.value = Math.max(Number(v.min), Math.min(Number(v.max), Number(v.value) + Number(b.dataset.vol)));
  if (volState) volState.muted = false;
  paintVol();
  clearTimeout(volTimer);
  volTimer = setTimeout(() => sendVol({ value: Number(v.value), muted: false }), 350);
}));
$("mute").addEventListener("click", () => {
  if (!volState) { sendKey("mute"); return; }
  const m = !(volState && volState.muted);
  if (volState) volState.muted = m;
  paintVol();
  sendVol({ muted: m });
});

/* ---------------- picture (Philips) ---------------- */
const STYLE_INFO = {
  en: { "Vivid": "Saturated", "Natural": "Realistic", "ECO": "Saves energy", "Movie": "Cinema tones", "Game": "Low latency",
        "Personal": "Your settings", "Monitor": "For PCs", "Bright": "Bright room", "Dark": "Dark room" },
  tr: { "Canlı": "Doygun renk", "Doğal": "Gerçekçi", "ECO": "Az enerji", "Film": "Sinema tonları", "Oyun": "Az gecikme",
        "Kişisel": "Senin ayarın", "Monitör": "Bilgisayar", "Parlak": "Aydınlık oda", "Koyu": "Karanlık oda" },
};
const STYLE_GRAD = [
  [/vivid|canlı|lebendig|vif|vivace/i, "linear-gradient(120deg,#ff3d6e,#ffb400,#1ed6ff)"],
  [/natur|doğal/i, "linear-gradient(120deg,#8fb9a8,#e8d9b5)"],
  [/eco/i, "linear-gradient(120deg,#1f8a4c,#7be08f)"],
  [/movie|film|cinema|kino/i, "linear-gradient(120deg,#2a1608,#c8782f)"],
  [/game|oyun|spiel|jeu/i, "linear-gradient(120deg,#5a22ff,#ff2bd6)"],
  [/personal|kişisel|persönlich/i, "linear-gradient(120deg,#6f86ff,#c77dff)"],
  [/monitor|monitör/i, "linear-gradient(120deg,#1b6dff,#c9e2ff)"],
  [/bright|parlak|hell/i, "linear-gradient(120deg,#ffcf5a,#fff3c4)"],
  [/dark|koyu|dunkel/i, "linear-gradient(120deg,#0b0d1a,#3d3f73)"],
];
const styleBase = n => n.replace(/^(HDR|HLG|Dolby Vision)\s+/, "");
// TV style names come in the TV's language; descriptions follow the panel's language.
const STYLE_KEY = { "vivid": "Vivid", "canlı": "Vivid", "natural": "Natural", "doğal": "Natural", "eco": "ECO",
  "movie": "Movie", "film": "Movie", "game": "Game", "oyun": "Game", "personal": "Personal", "kişisel": "Personal",
  "monitor": "Monitor", "monitör": "Monitor", "bright": "Bright", "parlak": "Bright", "dark": "Dark", "koyu": "Dark" };
const STYLE_DESC_TR = { Vivid: "Doygun renk", Natural: "Gerçekçi", ECO: "Az enerji", Movie: "Sinema tonları", Game: "Az gecikme",
  Personal: "Senin ayarın", Monitor: "Bilgisayar", Bright: "Aydınlık oda", Dark: "Karanlık oda" };
function styleDesc(n) {
  const k = STYLE_KEY[styleBase(n).toLocaleLowerCase("tr")] || STYLE_KEY[styleBase(n).toLowerCase()];
  if (!k) return "";
  return lang === "tr" ? STYLE_DESC_TR[k] : (STYLE_INFO.en[k] || "");
}
function styleGrad(n) { for (const [re, g] of STYLE_GRAD) if (re.test(n)) return g; return "linear-gradient(120deg,#555,#999)"; }
let picBusy = false, lastPicture = null;
function renderPicture(p) {
  lastPicture = p;
  const box = $("styles");
  box.innerHTML = "";
  const all = p.styles || [];
  const hidden = p.hidden || [];
  const dv = all.some(n => n.startsWith("Dolby Vision"));
  const hdr = dv || all.some(n => /^(HDR|HLG)/.test(n));
  $("picBadge").hidden = !hdr;
  $("picBadge").textContent = dv ? "DOLBY VISION" : "HDR";
  $("picBadge").className = "badge" + (dv ? " dv" : "");
  $("picNow").textContent = "";
  if (p.menu === false) {
    $("picNow").append("—");
    $("picNow").appendChild(el("small", "", t("pic.no_menu")));
  } else if (!all.length) {
    $("picNow").append("—");
    $("picNow").appendChild(el("small", "", t("pic.none")));
    const c = el("button", "chip", t("pic.read"));
    c.style.gridColumn = "1 / -1";
    c.addEventListener("click", () => busy(c, () => api("/api/picture/refresh", "POST")).finally(loadPicture));
    box.appendChild(c);
  } else {
    $("picNow").append(p.current || "—");
    $("picNow").appendChild(el("small", "", p.current ? styleDesc(p.current) : ""));
    all.filter(n => n === p.current || hidden.indexOf(styleBase(n)) < 0).forEach(name => {
      const tl = el("button", "tile" + (name === p.current ? " on" : ""));
      tl.style.setProperty("--g", styleGrad(name));
      tl.append(el("i"), el("b", "", styleBase(name)), el("span", "", styleDesc(name)));
      tl.addEventListener("click", () => applyStyle(name, tl, p.current));
      box.appendChild(tl);
    });
    box.appendChild(sceneTile(all, p.current));
    const n = box.children.length, rest = n % 3;
    if (rest) box.lastChild.style.gridColumn = "span " + (4 - rest);
  }
  if (p.brightness !== null && p.brightness !== undefined && !$("bright").matches(":active")) {
    $("bright").value = p.brightness;
    paintBright();
  }
}
function lockStyles(on, note) {
  picBusy = on;
  $("styles").classList.toggle("locked", on);
  $("picNote").textContent = on ? note : "";
}
function applyStyle(name, tile, current) {
  if (picBusy || name === current) return;
  lockStyles(true, t("pic.applying", styleBase(name)));
  busy(tile, async () => { toast((await api("/api/picture/style", "POST", { name })).msg); })
    .finally(() => { lockStyles(false); loadPicture(); });
}
// "Cinema": movie style (Dolby Vision "Dark" for HDR content) + Ambilight following the video.
function sceneTile(all, current) {
  const target = all.find(n => /movie|film|cinema|kino/i.test(styleBase(n))) || all.find(n => /dark|koyu|dunkel/i.test(styleBase(n)));
  const tl = el("button", "tile scene");
  tl.style.setProperty("--g", "linear-gradient(120deg,#1a0f05,#c8782f 55%,#6b85ff)");
  tl.append(el("i"), el("b", "", t("scene")), el("span", "", t("scene.desc", target ? styleBase(target) : "—")));
  tl.addEventListener("click", () => {
    if (picBusy) return;
    lockStyles(true, t("scene.busy"));
    busy(tl, async () => {
      if (target && target !== current) await api("/api/picture/style", "POST", { name: target });
      try { await api("/api/ambilight/video", "POST"); } catch (e) {}
      toast(t("scene.ok"));
    }).finally(() => { lockStyles(false); loadPicture(); refresh(); });
  });
  return tl;
}
function paintBright() {
  const v = $("bright").value;
  $("bright").style.setProperty("--p", v + "%");
  $("brightVal").textContent = v;
  document.querySelectorAll(".presets .chip").forEach(c => c.classList.toggle("on", Number(c.dataset.b) === Number(v)));
}
async function loadPicture() {
  if ($("tabPicture").hidden) return;
  try { const p = await api("/api/picture"); if (p.supported) renderPicture(p); } catch (e) {}
}
let brightTimer = null;
function sendBright(target) { return busy(target, async () => { toast((await api("/api/picture/brightness", "POST", { value: Number($("bright").value) })).msg); }); }
$("bright").addEventListener("input", paintBright);
$("bright").addEventListener("change", e => sendBright(e.target));
document.querySelectorAll(".step[data-step]").forEach(b => b.addEventListener("click", () => {
  $("bright").value = Math.max(0, Math.min(100, Number($("bright").value) + Number(b.dataset.step)));
  paintBright();
  clearTimeout(brightTimer);
  brightTimer = setTimeout(() => sendBright($("bright")), 450);
}));
document.querySelectorAll(".presets .chip").forEach(c => c.addEventListener("click", () => { $("bright").value = c.dataset.b; paintBright(); sendBright(c); }));

/* ---------------- Ambilight / Philips pairing ---------------- */
$("philipsHomePair").addEventListener("click", () => $("philipsPair").click());
$("philipsPair").addEventListener("click", e => busy(e.currentTarget, async () => {
  await api("/api/philips/pair/start", "POST");
  $("philipsPin").value = "";
  showSheet("pinSheet");
  setTimeout(() => $("philipsPin").focus(), 300);
}));
$("philipsPinDone").addEventListener("click", e => busy(e.currentTarget, async () => {
  const p = $("philipsPin").value.replace(/\D/g, "");
  if (p.length < 4) return;
  toast((await api("/api/philips/pair/finish", "POST", { pin: p })).msg);
  closeSheet();
  await refresh();
  loadPicture();
}));
$("ambPair").addEventListener("click", () => $("philipsPair").click());
document.querySelectorAll("[data-amb], [data-rgb]").forEach(b => b.addEventListener("click", e => {
  if (!b.parentElement.classList.contains("needpair")) return;
  e.stopImmediatePropagation();
  toast(t("amb.pair_first"));
  $("ambPair").focus();
}));
$("philipsDiag").addEventListener("click", e => busy(e.currentTarget, async () => {
  const d = await api("/api/philips/diag");
  delete d.ok;
  $("diagText").value = JSON.stringify(d, null, 2);
  showSheet("diagSheet");
}));
$("diagCopy").addEventListener("click", async () => {
  const ta = $("diagText");
  try { await navigator.clipboard.writeText(ta.value); toast(t("diag.copied")); return; } catch (e) {}
  ta.focus(); ta.select();
  let ok = false;
  try { ok = document.execCommand("copy"); } catch (e) {}
  toast(t(ok ? "diag.copied" : "diag.select"));
});
document.querySelectorAll("[data-amb]").forEach(b => b.addEventListener("click", () => busy(b, async () => {
  toast((await api("/api/ambilight/" + b.dataset.amb, "POST")).msg);
  setTimeout(refresh, 600);
})));
document.querySelectorAll("[data-rgb]").forEach(b => b.addEventListener("click", () => busy(b, async () => {
  const [r, g, bl] = b.dataset.rgb.split(",").map(Number);
  toast((await api("/api/ambilight/color", "POST", { r, g, b: bl })).msg);
})));

/* ---------------- keyboard ---------------- */
document.querySelectorAll('[data-open="kb"]').forEach(b => b.addEventListener("click", () => { showSheet("kbSheet"); setTimeout(() => $("kbText").focus(), 300); }));
$("kbSend").addEventListener("click", e => busy(e.currentTarget, async () => {
  const text = $("kbText").value;
  if (!text) return;
  const r = await api("/api/keyboard", "POST", { text });
  $("kbText").value = "";
  toast(r.msg);
}));
document.querySelectorAll("[data-key]").forEach(b => b.addEventListener("click", () => busy(b, () => api("/api/key/" + b.dataset.key, "POST"))));

/* ---------------- screenshot ---------------- */
async function loadShot() {
  $("shotSpin").style.display = "";
  $("shotImg").removeAttribute("src");
  try {
    const blob = await apiBlob("/api/screenshot");
    // data: URL so iOS offers "Add to Photos" on long-press.
    $("shotImg").src = await new Promise(res => { const f = new FileReader(); f.onload = () => res(f.result); f.readAsDataURL(blob); });
  } catch (e) { if (!(e instanceof ApiError && e.status === 401)) toast(e.message, true); }
  finally { $("shotSpin").style.display = "none"; }
}
$("openShot").addEventListener("click", () => { showSheet("shotSheet"); loadShot(); });
$("shotAgain").addEventListener("click", loadShot);

/* ---------------- party mode ---------------- */
let surpriseRunning = false;
function renderParty(enabled, running) {
  surpriseRunning = !!running;
  $("openSurprise").hidden = !enabled && !running;
  $("openSurprise").classList.toggle("running", surpriseRunning);
  $("surpriseLabel").textContent = t(surpriseRunning ? "party.running" : "party.label");
  $("surpriseSub").textContent = surpriseRunning ? t("party.stop") : "";
  $("partyTog").classList.toggle("on", !!enabled);
}
$("openSurprise").addEventListener("click", async () => {
  if (surpriseRunning) {
    await busy($("openSurprise"), async () => { await api("/api/surprise/stop", "POST"); renderParty(true, false); toast(t("party.stopped")); });
    return;
  }
  showSheet("surSheet");
});
$("surGo").addEventListener("click", e => busy(e.currentTarget, async () => {
  await api("/api/surprise", "POST", { message: $("surMsg").value.trim() || t("party.msg") });
  closeSheet();
  renderParty(true, true);
  toast(t("party.started"));
}));

/* ---------------- settings ---------------- */
let settings = null;
async function loadSettings() {
  try {
    settings = await api("/api/settings");
    renderDevices(settings.devices || []);
    $("partyTog").classList.toggle("on", !!settings.partyMode);
    $("updTog").classList.toggle("on", settings.updateCheck !== false);
  } catch (e) {}
  if (!allApps.length) await loadApps();
  renderFavList();
  loadLaunchers();
  loadAddons();
  loadDebloatCount();
  if (!$("tabPicture").hidden) { await loadPicture(); renderStyleList(); }
}
async function saveSettings(patch, quiet) {
  try { const r = await api("/api/settings", "POST", patch); if (!quiet) toast(r.msg); }
  catch (e) { toast(e.message, true); }
}
function renderFavList() {
  const order = $("favOrder"), rest = $("favList");
  order.innerHTML = ""; rest.innerHTML = "";
  const favs = allApps.filter(a => a.fav), others = allApps.filter(a => !a.fav);
  const saveOrder = () => saveSettings({ favorites: allApps.filter(x => x.fav).map(x => x.pkg) }, true).then(renderApps);
  const toggle = (a, on) => async () => {
    a.fav = on;
    if (on) { allApps.splice(allApps.indexOf(a), 1); const lastFav = allApps.filter(x => x.fav).length; allApps.splice(lastFav, 0, a); }
    await saveOrder();
    renderFavList();
  };
  if (!favs.length) { const r = el("div", "row"); r.appendChild(el("div", "hint", t("favs.none"))); order.appendChild(r); }
  favs.forEach(a => {
    const li = el("div", "row");
    li.dataset.pkg = a.pkg;
    const hd = el("span", "handle", "≡");
    const img = el("img"); img.alt = ""; iconFor(a.pkg, img);
    const g = el("div", "grow", a.label);
    const tog = el("button", "tog on"); tog.setAttribute("aria-label", a.label);
    tog.addEventListener("click", toggle(a, false));
    li.append(hd, img, g, tog);
    order.appendChild(li);
    hd.addEventListener("pointerdown", e => {
      e.preventDefault();
      li.classList.add("dragging");
      // Listen on the window: moving the row in the DOM drops any pointer capture on it.
      const move = ev => {
        ev.preventDefault();
        const rows = [...order.children];
        const over = rows.find(r => { const b = r.getBoundingClientRect(); return ev.clientY > b.top && ev.clientY < b.bottom; });
        if (over && over !== li) {
          const b = over.getBoundingClientRect();
          order.insertBefore(li, ev.clientY > b.top + b.height / 2 ? over.nextSibling : over);
        }
      };
      const up = () => {
        window.removeEventListener("pointermove", move);
        window.removeEventListener("pointerup", up);
        window.removeEventListener("pointercancel", up);
        li.classList.remove("dragging");
        const pkgs = [...order.children].map(r => r.dataset.pkg).filter(Boolean);
        const favApps = pkgs.map(p => allApps.find(x => x.pkg === p));
        allApps = favApps.concat(allApps.filter(x => !x.fav));
        saveOrder();
      };
      window.addEventListener("pointermove", move, { passive: false });
      window.addEventListener("pointerup", up);
      window.addEventListener("pointercancel", up);
    });
  });
  others.forEach(a => {
    const li = el("div", "row");
    const img = el("img"); img.alt = ""; iconFor(a.pkg, img);
    const tog = el("button", "tog"); tog.setAttribute("aria-label", a.label);
    tog.addEventListener("click", toggle(a, true));
    li.append(img, el("div", "grow", a.label), tog);
    rest.appendChild(li);
  });
}
function renderStyleList() {
  const box = $("styleList");
  box.innerHTML = "";
  if (!lastPicture || !(lastPicture.styles || []).length) { const r = el("div", "row"); r.appendChild(el("div", "hint", t("pic.none"))); box.appendChild(r); return; }
  const hidden = new Set(lastPicture.hidden || []);
  const bases = [...new Set(lastPicture.styles.map(styleBase))];
  bases.forEach(b => {
    const li = el("div", "row");
    const tog = el("button", "tog" + (hidden.has(b) ? "" : " on"));
    tog.setAttribute("aria-label", b);
    tog.addEventListener("click", async () => {
      if (hidden.has(b)) hidden.delete(b); else hidden.add(b);
      tog.classList.toggle("on", !hidden.has(b));
      lastPicture.hidden = [...hidden];
      await saveSettings({ hiddenStyles: [...hidden] }, true);
      renderPicture(lastPicture);
    });
    li.append(el("div", "grow", b), tog);
    box.appendChild(li);
  });
  const h = el("div", "row"); h.appendChild(el("div", "hint", t("styles.hint"))); box.appendChild(h);
}
function renderDevices(list) {
  const box = $("devList");
  box.innerHTML = "";
  list.forEach(d => {
    const li = el("div", "row");
    const g = el("div", "grow", d.name);
    g.appendChild(el("small", "", (d.current ? t("this.phone") + " · " : "") + new Date(d.created).toLocaleDateString(lang)));
    const rm = el("button", "link bad", t("remove"));
    rm.addEventListener("click", async () => {
      if (d.current && !confirm(t("unpair.confirm"))) return;
      try {
        await api("/api/devices/revoke", "POST", { id: d.id });
        if (d.current) { forgetCred(); showPairing(); } else loadSettings();
      } catch (e) { toast(e.message, true); }
    });
    li.append(g, rm);
    box.appendChild(li);
  });
}
/* ---------------- speed up (debloat suggestions) ---------------- */
let speedItems = [], speedSel = new Set(), speedMax = 10;
async function loadDebloatCount() {
  try {
    const d = await api("/api/debloat");
    speedItems = d.items || [];
    speedMax = d.max || 10;
    const n = speedItems.filter(i => i.enabled).length, off = speedItems.length - n;
    $("speedCount").textContent = String(n);
    $("speedCount").hidden = !n;
    $("speedSub").textContent = t("speed.sub.n", n, off);
  } catch (e) {}
}
function renderSpeed() {
  const box = $("speedList");
  box.innerHTML = "";
  const on = speedItems.filter(i => i.enabled);
  if (!on.length) box.appendChild(el("div", "hint", t("speed.none")));
  $("speedGo").hidden = !on.length;
  const groups = {};
  on.forEach(i => { (groups[i.group] = groups[i.group] || []).push(i); });
  Object.keys(groups).forEach(g => {
    box.appendChild(el("div", "grp", t("g." + g)));
    const list = el("div", "list");
    groups[g].forEach(i => {
      const row = el("div", "row");
      const txt = el("div", "grow", i.label);
      txt.appendChild(el("small", "", i.reason));
      const c = el("button", "check" + (speedSel.has(i.pkg) ? " on" : ""));
      c.setAttribute("aria-label", i.label);
      c.addEventListener("click", () => {
        if (speedSel.has(i.pkg)) speedSel.delete(i.pkg);
        else if (speedSel.size >= speedMax) { toast(t("speed.max", speedMax), true); return; }
        else speedSel.add(i.pkg);
        c.classList.toggle("on", speedSel.has(i.pkg));
        paintSpeedGo();
      });
      row.append(txt, c);
      list.appendChild(row);
    });
    box.appendChild(list);
  });
  const off = speedItems.filter(i => !i.enabled);
  $("speedOffBox").hidden = !off.length;
  const ol = $("speedOffList");
  ol.innerHTML = "";
  off.forEach(i => {
    const row = el("div", "row");
    const txt = el("div", "grow", i.label);
    txt.appendChild(el("small", "", i.reason));
    const b = el("button", "link", t("speed.on"));
    b.addEventListener("click", () => busy(b, async () => {
      toast((await api("/api/debloat/enable", "POST", { packages: [i.pkg] })).msg);
      await loadDebloatCount();
      renderSpeed();
    }));
    row.append(txt, b);
    ol.appendChild(row);
  });
  paintSpeedGo();
}
function paintSpeedGo() {
  $("speedGo").disabled = !speedSel.size;
  $("speedGo").textContent = speedSel.size ? t("speed.go.n", speedSel.size) : t("speed.go");
}
$("speedRow").addEventListener("click", async () => {
  speedSel = new Set();
  await loadDebloatCount();
  renderSpeed();
  showSheet("speedSheet");
});
$("speedGo").addEventListener("click", e => {
  if (!speedSel.size || !confirm(t("speed.confirm", speedSel.size))) return;
  busy(e.currentTarget, async () => {
    toast((await api("/api/debloat/disable", "POST", { packages: [...speedSel] })).msg);
    speedSel = new Set();
    await loadDebloatCount();
    renderSpeed();
  });
});

/* ---------------- extras (AirPlay) ---------------- */
let addonPoll = null;
const esc = (v) => String(v).replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
async function loadAddons() {
  let d;
  try { d = await api("/api/addons"); } catch (e) { return; }
  const a = d.airplay, box = $("addonList");
  box.innerHTML = "";
  const row = el("div", "row");
  const ic = el("span", "ic c-teal");
  ic.innerHTML = '<svg class="i" viewBox="0 0 24 24"><path d="M5 17H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-1"/><path d="M12 15l5 6H7z"/></svg>';
  const txt = el("div", "grow", t("airplay.title"));
  txt.appendChild(el("small", "", t("airplay.desc", a.sizeMb)));
  const busyState = a.state === "downloading" || a.state === "installing" || a.state === "starting";
  let right, how = false;
  if (busyState) {
    right = el("span", "meta spin", a.state === "downloading" ? t("airplay.dl", a.percent)
      : t(a.state === "starting" ? "airplay.starting" : "airplay.installing"));
  } else if (a.installed && a.running) {
    right = el("span", "meta ok", "✓ " + t("airplay.on"));
    how = true;
  } else if (a.installed) {
    txt.appendChild(el("small", "", t("airplay.off")));
    right = el("button", "btn fill", t("airplay.start"));
    right.addEventListener("click", () => busy(right, async () => {
      toast((await api("/api/addons/airplay/start", "POST")).msg);
      loadAddons();
    }));
  } else if (a.builtIn) {
    right = el("span", "meta ok", "✓ " + t("airplay.builtin"));
    how = true;
  } else {
    right = el("button", "btn fill", t("airplay.add"));
    right.addEventListener("click", () => busy(right, async () => {
      toast((await api("/api/addons/airplay", "POST")).msg);
      loadAddons();
    }));
  }
  if (a.error && !(a.installed && a.running)) txt.appendChild(el("small", "", a.error));
  row.append(ic, txt, right);
  box.appendChild(row);
  if (how) {
    // How to use it: the usual confusion is "installed, now what?". Name = what the iPhone lists.
    const h = el("div", "row"), hint = el("div", "hint");
    hint.innerHTML = t("airplay.how", esc(a.name || "Android AirPlay"));
    h.appendChild(hint);
    box.appendChild(h);
  }
  clearTimeout(addonPoll);
  if (busyState) addonPoll = setTimeout(loadAddons, 1500);
  else if (a.state === "done") loadApps();
}

/* ---------------- home screen (launcher) ---------------- */
async function loadLaunchers() {
  let d;
  try { d = await api("/api/launchers"); } catch (e) { return; }
  const box = $("homeList");
  box.innerHTML = "";
  d.items.forEach(h => {
    const li = el("div", "row");
    const pkg = h.component.split("/")[0];
    const name = pkg === "com.google.android.tvlauncher" ? t("home.atv") : pkg === "com.google.android.apps.tv.launcherx" ? t("home.gtv") : h.label;
    const g = el("div", "grow", name);
    let b;
    if (h.current) b = el("span", "meta ok", "✓ " + t("home.current"));
    else {
      b = el("button", "btn tint", t("home.use"));
      b.addEventListener("click", () => {
        if (!confirm(t("home.confirm", h.label))) return;
        busy(b, async () => {
          const r = await api("/api/launcher", "POST", { component: h.component });
          toast(r.msg);
          loadLaunchers();
        });
      });
    }
    li.append(g, b);
    box.appendChild(li);
  });
  (d.recommended || []).forEach(r => {
    const li = el("div", "row");
    const g = el("div", "grow", r.label);
    g.appendChild(el("small", "", t(r.desc)));
    const b = el("button", "btn tint", t("home.install"));
    b.addEventListener("click", () => busy(b, async () => { toast((await api("/api/launcher/install", "POST", { pkg: r.pkg })).msg); }));
    li.append(g, b);
    box.appendChild(li);
  });
  const onlyStock = d.items.every(h => h.stock) && !(d.recommended || []).length;
  $("homeHint").textContent = onlyStock ? t("home.none") : d.googleTv ? t("home.gtvnote") : d.canDisableStock ? t("home.note") : "";
  $("homeRestore").hidden = !d.changed;
}
$("homeRestore").addEventListener("click", e => {
  if (!confirm(t("home.restore.confirm"))) return;
  busy(e.currentTarget, async () => { toast((await api("/api/launcher/restore", "POST")).msg); loadLaunchers(); });
});
$("partyTog").addEventListener("click", async () => {
  const on = !$("partyTog").classList.contains("on");
  $("partyTog").classList.toggle("on", on);
  await saveSettings({ partyMode: on });
  refresh();
});
$("unpair").addEventListener("click", async () => {
  if (!confirm(t("unpair.confirm"))) return;
  try { await api("/api/devices/revoke", "POST", { id: cred.id }); } catch (e) {}
  forgetCred();
  showPairing();
});

/* ---------------- app updates ---------------- */
// The TV checks GitHub (at most daily, can be turned off); installing happens only on a tap here.
let updInfo = null, updPoll = null, updFrom = null, updGone = false;
const NOTES_RE = /^https:\/\/github\.com\/odinozlen\/odin-tv-magic\/releases\//;
function fmtDay(ms) { try { return new Date(ms).toLocaleDateString(lang); } catch (e) { return ""; } }
function renderUpdate(u) {
  if (!u) return;
  updInfo = u;
  if (updFrom && u.current && u.current !== updFrom) {  // the new version answers: load its panel
    try { sessionStorage.setItem("odintv-updated", u.current); } catch (e) {}
    location.reload();
    return;
  }
  const working = /^(downloading|verifying|installing)$/.test(u.state);
  const show = !!u.available || working || updGone;
  $("updTog").classList.toggle("on", !!u.enabled);
  $("updCheck").disabled = !u.enabled;
  $("updState").textContent = !u.enabled ? t("upd.off") : u.available ? t("upd.avail", u.latest)
    : u.checked ? t("upd.current", u.current, fmtDay(u.checked)) : t("upd.never");
  $("updCard").hidden = !show;
  clearTimeout(updPoll);
  if (!show) return;
  const adbOk = !!caps.adb;
  $("updTitle").textContent = t("upd.title", u.latest || "");
  $("updText").textContent = updGone ? t("upd.restarting")
    : u.state === "downloading" ? t("upd.dl", u.percent || 0)
    : u.state === "verifying" ? t("upd.verifying")
    : u.state === "installing" ? t("upd.installing")
    : u.state === "error" ? (u.error || t("upd.failed"))
    : adbOk ? t("upd.text", u.current) : t("upd.basic");
  $("updGo").hidden = !adbOk || working || updGone;
  const notes = u.notes_url && NOTES_RE.test(u.notes_url);
  $("updNotes").hidden = !notes;
  if (notes) $("updNotes").href = u.notes_url;
  if (working || updGone) updPoll = setTimeout(loadUpdate, updGone ? 2000 : 1200);
}
async function loadUpdate() {
  try {
    const u = await api("/api/update");
    updGone = false;
    renderUpdate(u);
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) return;
    // While the TV installs, the app is restarted: keep waiting for it.
    if (updFrom) { updGone = true; renderUpdate(updInfo); }
  }
}
$("updGo").addEventListener("click", e => {
  if (!updInfo || !confirm(t("upd.confirm", updInfo.latest))) return;
  busy(e.currentTarget, async () => {
    const r = await api("/api/update/install", "POST");
    updFrom = updInfo.current;
    toast(r.msg);
    await loadUpdate();
  });
});
$("updTog").addEventListener("click", async () => {
  const on = !$("updTog").classList.contains("on");
  $("updTog").classList.toggle("on", on);
  await saveSettings({ updateCheck: on });
  loadUpdate();
});
$("updCheck").addEventListener("click", e => busy(e.currentTarget, async () => {
  toast((await api("/api/update/check", "POST")).msg);
  await loadUpdate();
}));
function updatedNotice() {
  let v = null;
  try { v = sessionStorage.getItem("odintv-updated"); sessionStorage.removeItem("odintv-updated"); } catch (e) {}
  if (v) toast(t("upd.done", v));
}

function renderMadeBy() {
  const box = $("madeBy");
  box.textContent = "";
  box.append(t("made.by", AUTHOR.name ? AUTHOR.name + " (@" + AUTHOR.handle + ")" : "@" + AUTHOR.handle) + " · ");
  const mail = el("a", "link", AUTHOR.email);
  mail.href = "mailto:" + AUTHOR.email;
  const gh = el("a", "link", "GitHub");
  gh.href = AUTHOR.repo; gh.target = "_blank"; gh.rel = "noopener";
  box.append(mail, " · ", gh);
}

/* ---------------- add to home screen hint ---------------- */
function isComputer() {
  if (/Android|iPhone|iPad|iPod|Mobile/i.test(navigator.userAgent) || navigator.maxTouchPoints > 1) return false;
  return !window.matchMedia("(pointer: coarse)").matches;
}
function maybeA2hs() {
  const standalone = window.navigator.standalone || window.matchMedia("(display-mode: standalone)").matches;
  let seen = false;
  try { seen = localStorage.getItem("odintv-a2hs") === "1"; } catch (e) {}
  if (standalone || seen || isComputer()) return;
  const ios = /iPhone|iPad|iPod/.test(navigator.userAgent);
  $("a2hsText").innerHTML = t(ios ? "a2hs.ios" : "a2hs.android");
  showSheet("a2hsSheet");
}
$("a2hsOk").addEventListener("click", () => {
  try { localStorage.setItem("odintv-a2hs", "1"); } catch (e) {}
  closeSheet();
});

/* ---------------- start ---------------- */
let started = false;
async function start() {  // eslint-disable-line
  try { await syncClock(); } catch (e) { setOnline(false); $("now").textContent = t("tv.off"); }
  if (!cred) { showPairing(); return; }
  let tab = "home";
  try { tab = localStorage.getItem("odintv-tab") || "home"; } catch (e) {}
  await refresh();
  updatedNotice();
  await loadApps();
  loadDebloatCount();
  startOnboarding();
  showTab(tab);
  if (!started) {
    started = true;
    setInterval(() => { if (!document.hidden && cred) { refresh(); if (currentTab() === "remote") loadVolume(); } }, 15000);
    document.addEventListener("visibilitychange", () => {
      if (document.hidden || !cred) return;
      refresh();
      if (currentTab() === "remote") loadVolume();
      if (currentTab() === "picture") loadPicture();
    });
  }
}

applyI18n();
// QR code from the TV: http://<tv>:8080/#pair=123456
function pairFromHash() {
  const m = location.hash.match(/pair=(\d{6})/);
  if (!m) return false;
  history.replaceState(null, "", location.pathname);
  syncClock().catch(() => {}).finally(() => { showPairing(); $("pairCode").value = m[1]; pairWith(m[1]); });
  return true;
}
window.addEventListener("hashchange", pairFromHash);
// Opened on a computer (often from the link an AI assistant or the TV showed): point to the phone first.
// The one-time code in #pair= is not used here, so the TV's QR code still works on the phone.
function onDesktop() {
  try { if (localStorage.getItem("odintv-desktop") === "1") return false; } catch (e) {}
  return isComputer();
}
if (onDesktop()) {
  $("deskScreen").hidden = false;
  $("deskGo").addEventListener("click", () => {
    try { localStorage.setItem("odintv-desktop", "1"); } catch (e) {}
    $("deskScreen").hidden = true;
    if (!pairFromHash()) start();
  });
} else if (!pairFromHash()) start();
