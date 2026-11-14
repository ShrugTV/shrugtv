// ShrugTV phone companion (docs/companion_plan.md). Plain JS, no framework, no external requests
// except Twitch's image CDN. User content is always inserted as text, never as HTML.
'use strict';

const T = {
  en: {
    pairText: 'Scan the QR code on the TV, or enter the PIN shown there.', pinLabel: 'PIN', connect: 'Connect',
    pinWrong: 'PIN wrong or expired. Show a new code on the TV.', expired: 'This code has expired. Show a new one on the TV.',
    offline: 'TV not reachable. Is ShrugTV open?', tabRemote: 'Remote', tabChat: 'Chat', tabFind: 'Find', tabPlayer: 'Player',
    up: 'Up', down: 'Down', left: 'Left', right: 'Right', back: 'Back', more: 'More', playPause: 'Play/Pause',
    prev: '◀ Channel', live: 'Live', next: 'Channel ▶', volume: 'Stream volume', message: 'Message', send: 'Send',
    chatOff: 'Writing in the chat from the phone is switched off in the TV settings.', search: 'Search', liveNow: 'Live now',
    results: 'Results', quality: 'Quality', chatLayout: 'Chat layout', layoutSide: 'Next to the video',
    layoutOverlay: 'Over the video', layoutTransparent: 'Transparent', chatOn: 'Chat on', chatOffBtn: 'Chat off',
    stats: 'Statistics', close: 'Close player', openTwitch: 'Open on Twitch', nothing: 'Nothing playing',
    viewers: n => `${n.toLocaleString()} viewers`, since: m => `live for ${Math.floor(m / 60)}:${String(m % 60).padStart(2, '0')} h`,
    behind: s => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')} behind`, removed: 'message removed', offlineCh: 'offline',
    tabSettings: 'Settings', tabRemoteShort: 'Remote', tabChatShort: 'Chat', tabFindShort: 'Find', tabPlayerShort: 'Player', tabSettingsShort: 'Settings',
    remoteMode: 'Remote mode', modePad: 'Touchpad', modeKeys: 'Keys', padLabel: 'Touchpad: swipe for arrows, tap for OK',
    padHint: 'Swipe = arrow keys\nTap = OK · Two fingers = Back', replay: '⟲ Replay', audioOnly: 'Audio only',
    recent: 'Recently watched', replyTo: n => `Reply to ${n}`, replyCancel: 'Cancel reply', emotes: 'Emotes',
    noEmotes: 'No emotes seen in this chat yet. They appear here as soon as someone uses one.', quickAdd: 'Add a quick phrase',
    quickNew: 'New quick phrase (max. 60 characters)', quickRemove: t => `Remove "${t}" from the quick phrases?`,
    chatHint: 'Tap a message to answer it with @name. Hold a quick phrase to remove it.',
    tabUpdate: 'Version', tabUpdateShort: 'Info', verInstalled: 'Installed', verEdition: 'Edition', verEditionPublic: 'Public', verEditionPrivate: 'Private',
    verDevice: 'TV', verAndroid: 'Android', verLastCheck: 'Last checked', verNever: 'not yet', verCurrent: 'You have the newest version.',
    verUnknown: 'Not checked yet.', verUnsupported: 'This edition is not updated from the app, and there is no check for new versions here.',
    verAvailable: v => `Version ${v} is available`, verCheck: 'Check now', verChecking: 'Checking …', verInstall: 'Install update',
    verInstallConfirm: v => `Download version ${v} and start the installation on the TV? The TV asks you to confirm.`,
    verRelease: 'Release page', verInstallOff: 'Installing from the app is off. Switch it on in the TV settings: App → Install updates in the app.',
    verAutoOff: 'The daily check is off (Settings → About → Check for updates); "Check now" works anyway.',
    verDownloading: p => `Downloading … ${p} %`, verVerifying: 'Checked. Confirm the installation on the TV with OK.',
    verErrors: { disabled: 'Installing from the app is switched off.', none: 'No newer version known. Check first.', need_permission: 'Allow "Install unknown apps" for ShrugTV on the TV, then try again.',
      permission_unavailable: 'This TV has no screen for "Install unknown apps".', no_file: 'The release has no installable file.', bad_checksum: 'The checksum does not match, nothing was installed.',
      download_failed: 'The download failed.' },
    verNotes: 'What is new',
    favAdd: 'Mark as favorite', favRemove: 'Remove from favorites', favFailed: 'The star could not be saved.',
    listsLabel: 'Lists of the channel switcher', listFollowed: 'Followed', listSameGame: 'Same category', listSameGameLanguage: 'Same category, my language',
    listHistory: 'Recently watched', listTopStreams: 'Top streams', listTopLanguage: 'Top in my language',
    listEmpty: 'Nothing in this list right now.', listNeedsPlaying: 'This list needs a running stream: it shows the category of the channel that is playing.',
    homeTitle: 'On the home screen', homeHint: 'Open this page as an app icon:',
    homeIos: 'iPhone: Safari → Share → Add to Home Screen.', homeAndroid: 'Android: Chrome menu ⋮ → Add to Home screen.',
    searchChannels: 'Channel name', tabLog: 'Log',
    logOff: 'The log in the browser is switched off on the TV (Settings → Phone & browser → Log in the browser).',
    logAbout: "ShrugTV's own log on this TV, live. Tokens and passwords are always removed. Click a line for details.",
    logSearch: 'Search the log', logSource: 'Source', logAllSources: 'All sources', logLevel: 'Level',
    logAll: 'All', logErrors: 'Errors', logWarnings: 'Warnings', logInfo: 'Info', logDebug: 'Debug',
    logPause: 'Pause', logResume: 'Resume', logCopy: 'Copy', logExport: 'Save .txt', logPrivate: 'Hide private details',
    logLatest: 'Jump to latest ↓', logCopied: n => `${n} lines copied`, logSaved: n => `${n} lines saved`,
    logShown: (n, total) => `${n} of ${total} lines`, logEmpty: 'No lines match.', logPaused: 'paused',
    logTime: 'Time', logTag: 'Source', logThread: 'Process/thread', logMessage: 'Message', settingsOff: 'Changing settings from the browser is switched off on the TV (Settings → Phone & browser).',
    settingsSearch: 'Search settings', export: 'Save to file', import: 'Load from file', saved: 'Saved', notSaved: 'Not saved: value not allowed',
    imported: (n, m) => `${n} settings loaded` + (m ? `, ${m} skipped` : ''), importBad: 'This is not a ShrugTV settings file.',
    moveUp: 'Move up', run: 'Run', done: 'Done', tvOnly: 'Only on the TV', noScreen: 'Open ShrugTV on the TV first.', noResults: 'No settings found.',
    secretSet: 'stored – type to replace', secretEmpty: 'not set', on: 'On', off: 'Off', status: 'Status',
    pinned: 'Pinned', pin: 'Pin to the sidebar', unpin: 'Unpin', noPins: 'Pin settings with ☆ to find them here.',
    exported: n => `${n} settings saved to the file (without MQTT and companion, they belong to this TV).`,
    searchHint: '/ or Ctrl+K: search', theme: 'Light or dark', guide: 'Guide', guideShow: 'Show the guide',
    connection: 'Connection', mqttBroker: 'MQTT broker', noBroker: 'no broker address', entities: n => `${n} entities`,
    lastPublish: s => `sent ${ago(s, 'en')}`, connectedFor: s => `connected for ${ago(s, 'en', true)}`, deviceId: 'Device id',
    stateTopic: 'State', commandTopic: 'Commands', devicesTitle: 'Connected devices', thisDevice: 'this device', remove: 'Remove', justNow: 'just now',
    fromSkin: 'From the skin', designTitle: 'Own skins', designSave: 'Save current design as skin', designName: 'Name',
    designExport: 'Export (.json)', designImport: 'Import', designDelete: n => `Delete the skin ${n}?`, designFull: 'At most 10 own skins.',
    designBad: 'That is no ShrugTV design file.', designNone: 'No own skins yet.', designSaved: n => `Saved as ${n}.`, designImported: n => `Imported as ${n}.`,
    deviceSeen: (seen, paired) => `last used ${seen} · paired ${paired}`, removeDevice: n => `Remove ${n}? It will need the QR code or PIN again.`,
    noDevices: 'No devices remembered yet.', rememberOff: '"Remember connected devices" is off: devices need a code after every app start.', nameTaken: 'name used by another TV, code added', noTls: 'without TLS', reachable: 'reachable',
    mqttStates: { connected: 'connected', connecting: 'connecting …', off: 'off', 'no broker address': 'no broker address' }
  },
  de: {
    pairText: 'Scanne den QR-Code am TV oder gib die dort angezeigte PIN ein.', pinLabel: 'PIN', connect: 'Verbinden',
    pinWrong: 'PIN falsch oder abgelaufen. Zeig am TV einen neuen Code an.', expired: 'Dieser Code ist abgelaufen. Zeig am TV einen neuen an.',
    offline: 'TV nicht erreichbar. Ist ShrugTV offen?', tabRemote: 'Fernbedienung', tabChat: 'Chat', tabFind: 'Suchen', tabPlayer: 'Player',
    up: 'Hoch', down: 'Runter', left: 'Links', right: 'Rechts', back: 'Zurück', more: 'Mehr', playPause: 'Play/Pause',
    prev: '◀ Kanal', live: 'Live', next: 'Kanal ▶', volume: 'Stream-Lautstärke', message: 'Nachricht', send: 'Senden',
    chatOff: 'Chatten vom Handy ist in den Einstellungen des TVs ausgeschaltet.', search: 'Suchen', liveNow: 'Jetzt live',
    results: 'Ergebnisse', quality: 'Qualität', chatLayout: 'Chat-Layout', layoutSide: 'Neben dem Video',
    layoutOverlay: 'Über dem Video', layoutTransparent: 'Transparent', chatOn: 'Chat an', chatOffBtn: 'Chat aus',
    stats: 'Statistik', close: 'Player schließen', openTwitch: 'Auf Twitch öffnen', nothing: 'Es läuft nichts',
    viewers: n => `${n.toLocaleString('de-DE')} Zuschauer`, since: m => `seit ${Math.floor(m / 60)}:${String(m % 60).padStart(2, '0')} h`,
    behind: s => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')} zurück`, removed: 'Nachricht entfernt', offlineCh: 'offline',
    tabSettings: 'Einstellungen', tabRemoteShort: 'Steuern', tabChatShort: 'Chat', tabFindShort: 'Suchen', tabPlayerShort: 'Player', tabSettingsShort: 'Optionen',
    remoteMode: 'Fernbedienung', modePad: 'Touchpad', modeKeys: 'Tasten', padLabel: 'Touchpad: wischen für Pfeile, tippen für OK',
    padHint: 'Wischen = Pfeiltasten\nTippen = OK · Zwei Finger = Zurück', replay: '⟲ Replay', audioOnly: 'Nur Audio',
    recent: 'Zuletzt gesehen', replyTo: n => `Antwort an ${n}`, replyCancel: 'Antwort abbrechen', emotes: 'Emotes',
    noEmotes: 'In diesem Chat wurde noch kein Emote gesehen. Sie erscheinen hier, sobald jemand eins benutzt.', quickAdd: 'Schnellnachricht hinzufügen',
    quickNew: 'Neue Schnellnachricht (höchstens 60 Zeichen)', quickRemove: t => `„${t}“ aus den Schnellnachrichten entfernen?`,
    chatHint: 'Nachricht antippen, um mit @Name zu antworten. Schnellnachricht gedrückt halten zum Entfernen.',
    tabUpdate: 'Version', tabUpdateShort: 'Info', verInstalled: 'Installiert', verEdition: 'Edition', verEditionPublic: 'Öffentlich', verEditionPrivate: 'Privat',
    verDevice: 'Fernseher', verAndroid: 'Android', verLastCheck: 'Zuletzt geprüft', verNever: 'noch nicht', verCurrent: 'Du hast die neueste Version.',
    verUnknown: 'Noch nicht geprüft.', verUnsupported: 'Diese Edition wird nicht aus der App aktualisiert, und es gibt hier keine Prüfung auf neue Versionen.',
    verAvailable: v => `Version ${v} ist verfügbar`, verCheck: 'Jetzt prüfen', verChecking: 'Prüfe …', verInstall: 'Update installieren',
    verInstallConfirm: v => `Version ${v} laden und die Installation am Fernseher starten? Der Fernseher fragt dich nach der Bestätigung.`,
    verRelease: 'Release-Seite', verInstallOff: 'Das Installieren aus der App ist aus. Am Fernseher einschalten: App → Updates in der App installieren.',
    verAutoOff: 'Die tägliche Prüfung ist aus (Einstellungen → Über → Nach Updates suchen); „Jetzt prüfen“ geht trotzdem.',
    verDownloading: p => `Lade … ${p} %`, verVerifying: 'Geprüft. Bestätige die Installation am Fernseher mit OK.',
    verErrors: { disabled: 'Das Installieren aus der App ist ausgeschaltet.', none: 'Keine neuere Version bekannt. Erst prüfen.', need_permission: 'Erlaube am Fernseher „Unbekannte Apps installieren“ für ShrugTV und versuche es erneut.',
      permission_unavailable: 'Dieser Fernseher hat keinen Bildschirm für „Unbekannte Apps installieren“.', no_file: 'Das Release hat keine installierbare Datei.', bad_checksum: 'Die Prüfsumme stimmt nicht, es wurde nichts installiert.',
      download_failed: 'Der Download ist fehlgeschlagen.' },
    verNotes: 'Das ist neu',
    favAdd: 'Als Favorit markieren', favRemove: 'Aus den Favoriten nehmen', favFailed: 'Der Stern konnte nicht gespeichert werden.',
    listsLabel: 'Listen des Kanalwechslers', listFollowed: 'Gefolgt', listSameGame: 'Gleiche Kategorie', listSameGameLanguage: 'Gleiche Kategorie, meine Sprache',
    listHistory: 'Zuletzt gesehen', listTopStreams: 'Top-Streams', listTopLanguage: 'Top in meiner Sprache',
    listEmpty: 'Gerade nichts in dieser Liste.', listNeedsPlaying: 'Diese Liste braucht einen laufenden Stream: sie zeigt die Kategorie des Channels, der gerade läuft.',
    homeTitle: 'Auf dem Startbildschirm', homeHint: 'Diese Seite als App-Symbol öffnen:',
    homeIos: 'iPhone: Safari → Teilen → Zum Home-Bildschirm.', homeAndroid: 'Android: Chrome-Menü ⋮ → Zum Startbildschirm hinzufügen.',
    searchChannels: 'Kanalname', tabLog: 'Log',
    logOff: 'Das Log im Browser ist am TV ausgeschaltet (Einstellungen → Handy & Browser → Log im Browser).',
    logAbout: 'Das eigene Log von ShrugTV auf diesem TV, live. Tokens und Passwörter werden immer entfernt. Zeile anklicken für Details.',
    logSearch: 'Log durchsuchen', logSource: 'Quelle', logAllSources: 'Alle Quellen', logLevel: 'Stufe',
    logAll: 'Alle', logErrors: 'Fehler', logWarnings: 'Warnungen', logInfo: 'Info', logDebug: 'Debug',
    logPause: 'Pause', logResume: 'Weiter', logCopy: 'Kopieren', logExport: 'Als .txt speichern', logPrivate: 'Private Details ausblenden',
    logLatest: 'Zum Neuesten ↓', logCopied: n => `${n} Zeilen kopiert`, logSaved: n => `${n} Zeilen gespeichert`,
    logShown: (n, total) => `${n} von ${total} Zeilen`, logEmpty: 'Keine passenden Zeilen.', logPaused: 'pausiert',
    logTime: 'Zeit', logTag: 'Quelle', logThread: 'Prozess/Thread', logMessage: 'Nachricht', settingsOff: 'Einstellungen vom Browser sind am TV ausgeschaltet (Einstellungen → Handy & Browser).',
    settingsSearch: 'Einstellungen suchen', export: 'In Datei speichern', import: 'Aus Datei laden', saved: 'Gespeichert', notSaved: 'Nicht gespeichert: Wert nicht erlaubt',
    imported: (n, m) => `${n} Einstellungen geladen` + (m ? `, ${m} übersprungen` : ''), importBad: 'Das ist keine ShrugTV-Einstellungsdatei.',
    moveUp: 'Nach oben', run: 'Ausführen', done: 'Erledigt', tvOnly: 'Nur am TV', noScreen: 'Öffne zuerst ShrugTV am TV.', noResults: 'Keine Einstellungen gefunden.',
    secretSet: 'gespeichert – tippen zum Ersetzen', secretEmpty: 'nicht gesetzt', on: 'An', off: 'Aus', status: 'Status',
    pinned: 'Angeheftet', pin: 'An die Seitenleiste heften', unpin: 'Lösen', noPins: 'Hefte Einstellungen mit ☆ an, um sie hier zu finden.',
    exported: n => `${n} Einstellungen in die Datei gespeichert (ohne MQTT und Handy & Browser, die gehören zu diesem TV).`,
    searchHint: '/ oder Strg+K: suchen', theme: 'Hell oder dunkel', guide: 'Anleitung', guideShow: 'Anleitung zeigen',
    connection: 'Verbindung', mqttBroker: 'MQTT-Broker', noBroker: 'keine Broker-Adresse', entities: n => `${n} Entitäten`,
    lastPublish: s => `gesendet ${ago(s, 'de')}`, connectedFor: s => `verbunden seit ${ago(s, 'de', true)}`, deviceId: 'Geräte-ID',
    stateTopic: 'Status', commandTopic: 'Befehle', devicesTitle: 'Verbundene Geräte', thisDevice: 'dieses Gerät', remove: 'Entfernen', justNow: 'gerade eben',
    fromSkin: 'Vom Skin', designTitle: 'Eigene Skins', designSave: 'Aktuelles Design als Skin speichern', designName: 'Name',
    designExport: 'Exportieren (.json)', designImport: 'Importieren', designDelete: n => `Skin ${n} löschen?`, designFull: 'Höchstens 10 eigene Skins.',
    designBad: 'Das ist keine ShrugTV-Designdatei.', designNone: 'Noch keine eigenen Skins.', designSaved: n => `Gespeichert als ${n}.`, designImported: n => `Importiert als ${n}.`,
    deviceSeen: (seen, paired) => `zuletzt ${seen} · gekoppelt ${paired}`, removeDevice: n => `${n} entfernen? Es braucht dann wieder QR-Code oder PIN.`,
    noDevices: 'Noch keine Geräte gemerkt.', rememberOff: '„Verbundene Geräte merken“ ist aus: Geräte brauchen nach jedem App-Start einen Code.', nameTaken: 'Name von anderem TV belegt, Kürzel angehängt', noTls: 'ohne TLS', reachable: 'erreichbar',
    mqttStates: { connected: 'verbunden', connecting: 'verbinde …', off: 'aus', 'no broker address': 'keine Broker-Adresse' }
  },
  es: {
    pairText: 'Escanea el código QR en el TV o introduce el PIN que se muestra allí.', pinLabel: 'PIN', connect: 'Conectar',
    pinWrong: 'PIN incorrecto o caducado. Muestra un código nuevo en el TV.', expired: 'Este código ha caducado. Muestra uno nuevo en el TV.',
    offline: 'No se puede acceder al TV. ¿Está abierto ShrugTV?', tabRemote: 'Mando', tabChat: 'Chat', tabFind: 'Buscar', tabPlayer: 'Reproductor',
    up: 'Arriba', down: 'Abajo', left: 'Izquierda', right: 'Derecha', back: 'Atrás', more: 'Más', playPause: 'Reproducir/Pausa',
    prev: '◀ Canal', live: 'Directo', next: 'Canal ▶', volume: 'Volumen del stream', message: 'Mensaje', send: 'Enviar',
    chatOff: 'Escribir en el chat desde el móvil está desactivado en los ajustes del TV.', search: 'Buscar', liveNow: 'En directo ahora',
    results: 'Resultados', quality: 'Calidad', chatLayout: 'Disposición del chat', layoutSide: 'Junto al vídeo',
    layoutOverlay: 'Sobre el vídeo', layoutTransparent: 'Transparente', chatOn: 'Chat activado', chatOffBtn: 'Chat desactivado',
    stats: 'Estadísticas', close: 'Cerrar reproductor', openTwitch: 'Abrir en Twitch', nothing: 'No se reproduce nada',
    viewers: n => `${n.toLocaleString()} espectadores`, since: m => `en directo desde hace ${Math.floor(m / 60)}:${String(m % 60).padStart(2, '0')} h`,
    behind: s => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')} de retraso`, removed: 'mensaje eliminado', offlineCh: 'desconectado',
    tabSettings: 'Ajustes', tabRemoteShort: 'Mando', tabChatShort: 'Chat', tabFindShort: 'Buscar', tabPlayerShort: 'Reproductor', tabSettingsShort: 'Ajustes',
    searchChannels: 'Nombre del canal', tabLog: 'Registro',
    logOff: 'El registro en el navegador está desactivado en el TV (Ajustes → Móvil y navegador → Registro en el navegador).',
    logAbout: "El registro propio de ShrugTV en este TV, en directo. Los tokens y las contraseñas siempre se eliminan. Haz clic en una línea para ver detalles.",
    logSearch: 'Buscar en el registro', logSource: 'Origen', logAllSources: 'Todos los orígenes', logLevel: 'Nivel',
    logAll: 'Todo', logErrors: 'Errores', logWarnings: 'Advertencias', logInfo: 'Info', logDebug: 'Depuración',
    logPause: 'Pausar', logResume: 'Reanudar', logCopy: 'Copiar', logExport: 'Guardar .txt', logPrivate: 'Ocultar datos privados',
    logLatest: 'Ir al último ↓', logCopied: n => `${n} líneas copiadas`, logSaved: n => `${n} líneas guardadas`,
    logShown: (n, total) => `${n} de ${total} líneas`, logEmpty: 'Ninguna línea coincide.', logPaused: 'en pausa',
    logTime: 'Hora', logTag: 'Origen', logThread: 'Proceso/hilo', logMessage: 'Mensaje', settingsOff: 'Cambiar los ajustes desde el navegador está desactivado en el TV (Ajustes → Móvil y navegador).',
    settingsSearch: 'Buscar ajustes', export: 'Guardar en archivo', import: 'Cargar desde archivo', saved: 'Guardado', notSaved: 'No guardado: valor no permitido',
    imported: (n, m) => `${n} ajustes cargados` + (m ? `, ${m} omitidos` : ''), importBad: 'Este no es un archivo de ajustes de ShrugTV.',
    moveUp: 'Subir', run: 'Ejecutar', done: 'Listo', tvOnly: 'Solo en el TV', noScreen: 'Abre primero ShrugTV en el TV.', noResults: 'No se encontraron ajustes.',
    secretSet: 'guardado – escribe para reemplazar', secretEmpty: 'sin definir', on: 'Activado', off: 'Desactivado', status: 'Estado',
    pinned: 'Fijados', pin: 'Fijar en la barra lateral', unpin: 'Desfijar', noPins: 'Fija ajustes con ☆ para encontrarlos aquí.',
    exported: n => `${n} ajustes guardados en el archivo (sin MQTT ni móvil, pertenecen a este TV).`,
    searchHint: '/ o Ctrl+K: buscar', theme: 'Claro u oscuro', guide: 'Guía', guideShow: 'Mostrar la guía',
    connection: 'Conexión', mqttBroker: 'Broker MQTT', noBroker: 'sin dirección de broker', entities: n => `${n} entidades`,
    lastPublish: s => `enviado ${ago(s, 'es')}`, connectedFor: s => `conectado desde hace ${ago(s, 'es', true)}`, deviceId: 'ID del dispositivo',
    stateTopic: 'Estado', commandTopic: 'Comandos', devicesTitle: 'Dispositivos conectados', thisDevice: 'este dispositivo', remove: 'Eliminar', justNow: 'ahora mismo',
    fromSkin: 'Del tema', designTitle: 'Temas propios', designSave: 'Guardar el diseño actual como tema', designName: 'Nombre',
    designExport: 'Exportar (.json)', designImport: 'Importar', designDelete: n => `¿Eliminar el tema ${n}?`, designFull: 'Como máximo 10 temas propios.',
    designBad: 'Ese no es un archivo de diseño de ShrugTV.', designNone: 'Aún no hay temas propios.', designSaved: n => `Guardado como ${n}.`, designImported: n => `Importado como ${n}.`,
    deviceSeen: (seen, paired) => `último uso ${seen} · emparejado ${paired}`, removeDevice: n => `¿Eliminar ${n}? Necesitará de nuevo el código QR o el PIN.`,
    noDevices: 'Aún no hay dispositivos recordados.', rememberOff: '"Recordar dispositivos conectados" está desactivado: los dispositivos necesitan un código tras cada inicio de la app.', nameTaken: 'nombre usado por otro TV, código añadido', noTls: 'sin TLS', reachable: 'accesible',
    mqttStates: { connected: 'conectado', connecting: 'conectando …', off: 'desactivado', 'no broker address': 'sin dirección de broker' },
    remoteMode: 'Modo de mando', modePad: 'Panel táctil', modeKeys: 'Teclas', padLabel: 'Panel táctil: desliza para las flechas, toca para OK',
    padHint: 'Deslizar = flechas\nTocar = OK · Dos dedos = Atrás', replay: '⟲ Repetición', audioOnly: 'Solo audio',
    recent: 'Vistos recientemente', replyTo: n => `Responder a ${n}`, replyCancel: 'Cancelar respuesta', emotes: 'Emotes',
    noEmotes: 'Aún no se ha visto ningún emote en este chat. Aparecen aquí en cuanto alguien usa uno.', quickAdd: 'Añadir una frase rápida',
    quickNew: 'Nueva frase rápida (máx. 60 caracteres)', quickRemove: t => `¿Eliminar "${t}" de las frases rápidas?`,
    chatHint: 'Toca un mensaje para responder con @nombre. Mantén pulsada una frase rápida para eliminarla.',
    tabUpdate: 'Versión', tabUpdateShort: 'Info', verInstalled: 'Instalada', verEdition: 'Edición', verEditionPublic: 'Pública', verEditionPrivate: 'Privada',
    verDevice: 'TV', verAndroid: 'Android', verLastCheck: 'Última comprobación', verNever: 'aún no', verCurrent: 'Tienes la versión más reciente.',
    verUnknown: 'Aún no se ha comprobado.', verUnsupported: 'Esta edición no se actualiza desde la app y aquí no hay comprobación de versiones nuevas.',
    verAvailable: v => `Hay una versión ${v} disponible`, verCheck: 'Comprobar ahora', verChecking: 'Comprobando …', verInstall: 'Instalar actualización',
    verInstallConfirm: v => `¿Descargar la versión ${v} e iniciar la instalación en el TV? El TV te pedirá que la confirmes.`,
    verRelease: 'Página de la versión', verInstallOff: 'La instalación desde la app está desactivada. Actívala en los ajustes del TV: App → Instalar actualizaciones en la app.',
    verAutoOff: 'La comprobación diaria está desactivada (Ajustes → Acerca de → Buscar actualizaciones); "Comprobar ahora" funciona igualmente.',
    verDownloading: p => `Descargando … ${p} %`, verVerifying: 'Verificado. Confirma la instalación en el TV con OK.',
    verErrors: { disabled: 'La instalación desde la app está desactivada.', none: 'No se conoce ninguna versión más reciente. Comprueba primero.', need_permission: 'Permite "Instalar apps desconocidas" para ShrugTV en el TV y vuelve a intentarlo.',
      permission_unavailable: 'Este TV no tiene pantalla para "Instalar apps desconocidas".', no_file: 'La versión no tiene ningún archivo instalable.', bad_checksum: 'La suma de comprobación no coincide, no se instaló nada.',
      download_failed: 'La descarga ha fallado.' },
    verNotes: 'Novedades',
    favAdd: 'Marcar como favorito', favRemove: 'Quitar de favoritos', favFailed: 'No se pudo guardar la estrella.',
    listsLabel: 'Listas del cambiador de canales', listFollowed: 'Seguidos', listSameGame: 'Misma categoría', listSameGameLanguage: 'Misma categoría, mi idioma',
    listHistory: 'Vistos recientemente', listTopStreams: 'Streams más vistos', listTopLanguage: 'Más vistos en mi idioma',
    listEmpty: 'Ahora mismo no hay nada en esta lista.', listNeedsPlaying: 'Esta lista necesita un stream en reproducción: muestra la categoría del canal que se está viendo.',
    homeTitle: 'En la pantalla de inicio', homeHint: 'Abre esta página como icono de app:',
    homeIos: 'iPhone: Safari → Compartir → Añadir a pantalla de inicio.', homeAndroid: 'Android: menú de Chrome ⋮ → Añadir a pantalla de inicio.'
  },
  fr: {
    pairText: 'Scannez le code QR sur la TV, ou saisissez le code PIN qui y est affiché.', pinLabel: 'PIN', connect: 'Connecter',
    pinWrong: 'PIN incorrect ou expiré. Affichez un nouveau code sur la TV.', expired: 'Ce code a expiré. Affichez-en un nouveau sur la TV.',
    offline: 'TV inaccessible. ShrugTV est-il ouvert ?', tabRemote: 'Télécommande', tabChat: 'Chat', tabFind: 'Recherche', tabPlayer: 'Lecteur',
    up: 'Haut', down: 'Bas', left: 'Gauche', right: 'Droite', back: 'Retour', more: 'Plus', playPause: 'Lecture/Pause',
    prev: '◀ Chaîne', live: 'Direct', next: 'Chaîne ▶', volume: 'Volume du stream', message: 'Message', send: 'Envoyer',
    chatOff: 'L\'écriture dans le chat depuis le téléphone est désactivée dans les réglages de la TV.', search: 'Rechercher', liveNow: 'En direct maintenant',
    results: 'Résultats', quality: 'Qualité', chatLayout: 'Disposition du chat', layoutSide: 'À côté de la vidéo',
    layoutOverlay: 'Par-dessus la vidéo', layoutTransparent: 'Transparent', chatOn: 'Chat activé', chatOffBtn: 'Chat désactivé',
    stats: 'Statistiques', close: 'Fermer le lecteur', openTwitch: 'Ouvrir sur Twitch', nothing: 'Aucune lecture en cours',
    viewers: n => `${n.toLocaleString()} spectateurs`, since: m => `en direct depuis ${Math.floor(m / 60)}:${String(m % 60).padStart(2, '0')} h`,
    behind: s => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')} de retard`, removed: 'message supprimé', offlineCh: 'hors ligne',
    tabSettings: 'Réglages', tabRemoteShort: 'Télécommande', tabChatShort: 'Chat', tabFindShort: 'Recherche', tabPlayerShort: 'Lecteur', tabSettingsShort: 'Réglages',
    searchChannels: 'Nom de la chaîne', tabLog: 'Journal',
    logOff: 'Le journal dans le navigateur est désactivé sur la TV (Réglages → Téléphone et navigateur → Journal dans le navigateur).',
    logAbout: "Le journal propre à ShrugTV sur cette TV, en direct. Les jetons et mots de passe sont toujours supprimés. Cliquez sur une ligne pour les détails.",
    logSearch: 'Rechercher dans le journal', logSource: 'Source', logAllSources: 'Toutes les sources', logLevel: 'Niveau',
    logAll: 'Tous', logErrors: 'Erreurs', logWarnings: 'Avertissements', logInfo: 'Infos', logDebug: 'Débogage',
    logPause: 'Pause', logResume: 'Reprendre', logCopy: 'Copier', logExport: 'Enregistrer en .txt', logPrivate: 'Masquer les détails privés',
    logLatest: 'Aller au plus récent ↓', logCopied: n => `${n} lignes copiées`, logSaved: n => `${n} lignes enregistrées`,
    logShown: (n, total) => `${n} lignes sur ${total}`, logEmpty: 'Aucune ligne ne correspond.', logPaused: 'en pause',
    logTime: 'Heure', logTag: 'Source', logThread: 'Processus/thread', logMessage: 'Message', settingsOff: 'La modification des réglages depuis le navigateur est désactivée sur la TV (Réglages → Téléphone et navigateur).',
    settingsSearch: 'Rechercher dans les réglages', export: 'Enregistrer dans un fichier', import: 'Charger depuis un fichier', saved: 'Enregistré', notSaved: 'Non enregistré : valeur non autorisée',
    imported: (n, m) => `${n} réglages chargés` + (m ? `, ${m} ignorés` : ''), importBad: 'Ce n\'est pas un fichier de réglages ShrugTV.',
    moveUp: 'Monter', run: 'Exécuter', done: 'Terminé', tvOnly: 'Uniquement sur la TV', noScreen: 'Ouvrez d\'abord ShrugTV sur la TV.', noResults: 'Aucun réglage trouvé.',
    secretSet: 'enregistré – saisissez pour remplacer', secretEmpty: 'non défini', on: 'Activé', off: 'Désactivé', status: 'État',
    pinned: 'Épinglés', pin: 'Épingler à la barre latérale', unpin: 'Désépingler', noPins: 'Épinglez des réglages avec ☆ pour les retrouver ici.',
    exported: n => `${n} réglages enregistrés dans le fichier (sans MQTT ni téléphone et navigateur, qui appartiennent à cette TV).`,
    searchHint: '/ ou Ctrl+K : rechercher', theme: 'Clair ou sombre', guide: 'Guide', guideShow: 'Afficher le guide',
    connection: 'Connexion', mqttBroker: 'Broker MQTT', noBroker: 'pas d\'adresse de broker', entities: n => `${n} entités`,
    lastPublish: s => `envoyé ${ago(s, 'fr')}`, connectedFor: s => `connecté depuis ${ago(s, 'fr', true)}`, deviceId: 'Identifiant de l\'appareil',
    stateTopic: 'État', commandTopic: 'Commandes', devicesTitle: 'Appareils connectés', thisDevice: 'cet appareil', remove: 'Supprimer', justNow: 'à l\'instant',
    fromSkin: 'Du thème', designTitle: 'Thèmes personnels', designSave: 'Enregistrer le design actuel comme thème', designName: 'Nom',
    designExport: 'Exporter (.json)', designImport: 'Importer', designDelete: n => `Supprimer le thème ${n} ?`, designFull: '10 thèmes personnels au maximum.',
    designBad: 'Ce n\'est pas un fichier de design ShrugTV.', designNone: 'Aucun thème personnel pour l\'instant.', designSaved: n => `Enregistré sous ${n}.`, designImported: n => `Importé sous ${n}.`,
    deviceSeen: (seen, paired) => `dernière utilisation ${seen} · associé ${paired}`, removeDevice: n => `Supprimer ${n} ? Il aura de nouveau besoin du code QR ou du PIN.`,
    noDevices: 'Aucun appareil mémorisé pour l\'instant.', rememberOff: '« Mémoriser les appareils connectés » est désactivé : les appareils ont besoin d\'un code après chaque démarrage de l\'application.', nameTaken: 'nom utilisé par une autre TV, code ajouté', noTls: 'sans TLS', reachable: 'accessible',
    mqttStates: { connected: 'connecté', connecting: 'connexion …', off: 'désactivé', 'no broker address': 'pas d\'adresse de broker' },
    remoteMode: 'Mode télécommande', modePad: 'Pavé tactile', modeKeys: 'Touches', padLabel: 'Pavé tactile : glissez pour les flèches, touchez pour OK',
    padHint: 'Glisser = flèches\nToucher = OK · Deux doigts = Retour', replay: '⟲ Replay', audioOnly: 'Audio seul',
    recent: 'Vu récemment', replyTo: n => `Répondre à ${n}`, replyCancel: 'Annuler la réponse', emotes: 'Émotes',
    noEmotes: "Aucune émote vue dans ce chat pour l'instant. Elles apparaissent ici dès que quelqu'un en utilise une.", quickAdd: 'Ajouter une phrase rapide',
    quickNew: 'Nouvelle phrase rapide (60 caractères max.)', quickRemove: t => `Supprimer « ${t} » des phrases rapides ?`,
    chatHint: "Touchez un message pour répondre avec @nom. Maintenez une phrase rapide pour la supprimer.",
    tabUpdate: 'Version', tabUpdateShort: 'Info', verInstalled: 'Installée', verEdition: 'Édition', verEditionPublic: 'Publique', verEditionPrivate: 'Privée',
    verDevice: 'TV', verAndroid: 'Android', verLastCheck: 'Dernière vérification', verNever: 'pas encore', verCurrent: 'Vous avez la version la plus récente.',
    verUnknown: 'Pas encore vérifié.', verUnsupported: "Cette édition n'est pas mise à jour depuis l'app, et il n'y a pas de recherche de nouvelles versions ici.",
    verAvailable: v => `La version ${v} est disponible`, verCheck: 'Vérifier maintenant', verChecking: 'Vérification …', verInstall: 'Installer la mise à jour',
    verInstallConfirm: v => `Télécharger la version ${v} et lancer l'installation sur la TV ? La TV vous demande de confirmer.`,
    verRelease: 'Page de la version', verInstallOff: "L'installation depuis l'app est désactivée. Activez-la dans les réglages de la TV : App → Installer les mises à jour dans l'app.",
    verAutoOff: 'La vérification quotidienne est désactivée (Réglages → À propos → Rechercher des mises à jour) ; « Vérifier maintenant » fonctionne quand même.',
    verDownloading: p => `Téléchargement … ${p} %`, verVerifying: "Vérifié. Confirmez l'installation sur la TV avec OK.",
    verErrors: { disabled: "L'installation depuis l'app est désactivée.", none: "Aucune version plus récente connue. Vérifiez d'abord.", need_permission: 'Autorisez « Installer des apps inconnues » pour ShrugTV sur la TV, puis réessayez.',
      permission_unavailable: "Cette TV n'a pas d'écran pour « Installer des apps inconnues ».", no_file: "La version n'a aucun fichier installable.", bad_checksum: "La somme de contrôle ne correspond pas, rien n'a été installé.",
      download_failed: 'Le téléchargement a échoué.' },
    verNotes: 'Nouveautés',
    favAdd: 'Marquer comme favori', favRemove: 'Retirer des favoris', favFailed: "L'étoile n'a pas pu être enregistrée.",
    listsLabel: 'Listes du sélecteur de chaînes', listFollowed: 'Suivies', listSameGame: 'Même catégorie', listSameGameLanguage: 'Même catégorie, ma langue',
    listHistory: 'Vu récemment', listTopStreams: 'Top streams', listTopLanguage: 'Top dans ma langue',
    listEmpty: 'Rien dans cette liste pour le moment.', listNeedsPlaying: "Cette liste nécessite un stream en cours : elle montre la catégorie de la chaîne qui est lue.",
    homeTitle: "Sur l'écran d'accueil", homeHint: "Ouvrez cette page comme une icône d'app :",
    homeIos: "iPhone : Safari → Partager → Sur l'écran d'accueil.", homeAndroid: "Android : menu de Chrome ⋮ → Ajouter à l'écran d'accueil."
  },
  pt: {
    pairText: 'Escaneie o QR code na TV ou digite o PIN exibido lá.', pinLabel: 'PIN', connect: 'Conectar',
    pinWrong: 'PIN incorreto ou expirado. Mostre um novo código na TV.', expired: 'Este código expirou. Mostre um novo na TV.',
    offline: 'TV inacessível. O ShrugTV está aberto?', tabRemote: 'Controle', tabChat: 'Chat', tabFind: 'Buscar', tabPlayer: 'Player',
    up: 'Cima', down: 'Baixo', left: 'Esquerda', right: 'Direita', back: 'Voltar', more: 'Mais', playPause: 'Reproduzir/Pausar',
    prev: '◀ Canal', live: 'Ao vivo', next: 'Canal ▶', volume: 'Volume da transmissão', message: 'Mensagem', send: 'Enviar',
    chatOff: 'Escrever no chat pelo celular está desativado nas configurações da TV.', search: 'Buscar', liveNow: 'Ao vivo agora',
    results: 'Resultados', quality: 'Qualidade', chatLayout: 'Layout do chat', layoutSide: 'Ao lado do vídeo',
    layoutOverlay: 'Sobre o vídeo', layoutTransparent: 'Transparente', chatOn: 'Chat ligado', chatOffBtn: 'Chat desligado',
    stats: 'Estatísticas', close: 'Fechar player', openTwitch: 'Abrir na Twitch', nothing: 'Nada em reprodução',
    viewers: n => `${n.toLocaleString()} espectadores`, since: m => `ao vivo há ${Math.floor(m / 60)}:${String(m % 60).padStart(2, '0')} h`,
    behind: s => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')} atrás`, removed: 'mensagem removida', offlineCh: 'offline',
    tabSettings: 'Configurações', tabRemoteShort: 'Controle', tabChatShort: 'Chat', tabFindShort: 'Buscar', tabPlayerShort: 'Player', tabSettingsShort: 'Config.',
    searchChannels: 'Nome do canal', tabLog: 'Log',
    logOff: 'O log no navegador está desativado na TV (Configurações → Celular e navegador → Log no navegador).',
    logAbout: "Log do próprio ShrugTV nesta TV, ao vivo. Tokens e senhas são sempre removidos. Clique em uma linha para ver detalhes.",
    logSearch: 'Buscar no log', logSource: 'Origem', logAllSources: 'Todas as origens', logLevel: 'Nível',
    logAll: 'Todos', logErrors: 'Erros', logWarnings: 'Avisos', logInfo: 'Info', logDebug: 'Debug',
    logPause: 'Pausar', logResume: 'Retomar', logCopy: 'Copiar', logExport: 'Salvar .txt', logPrivate: 'Ocultar detalhes privados',
    logLatest: 'Ir para o mais recente ↓', logCopied: n => `${n} linhas copiadas`, logSaved: n => `${n} linhas salvas`,
    logShown: (n, total) => `${n} de ${total} linhas`, logEmpty: 'Nenhuma linha corresponde.', logPaused: 'pausado',
    logTime: 'Hora', logTag: 'Origem', logThread: 'Processo/thread', logMessage: 'Mensagem', settingsOff: 'Alterar configurações pelo navegador está desativado na TV (Configurações → Celular e navegador).',
    settingsSearch: 'Buscar configurações', export: 'Salvar em arquivo', import: 'Carregar de arquivo', saved: 'Salvo', notSaved: 'Não salvo: valor não permitido',
    imported: (n, m) => `${n} configurações carregadas` + (m ? `, ${m} ignoradas` : ''), importBad: 'Este não é um arquivo de configurações do ShrugTV.',
    moveUp: 'Subir', run: 'Executar', done: 'Concluído', tvOnly: 'Só na TV', noScreen: 'Abra o ShrugTV na TV primeiro.', noResults: 'Nenhuma configuração encontrada.',
    secretSet: 'armazenado – digite para substituir', secretEmpty: 'não definido', on: 'Ligado', off: 'Desligado', status: 'Status',
    pinned: 'Fixadas', pin: 'Fixar na barra lateral', unpin: 'Desafixar', noPins: 'Fixe configurações com ☆ para encontrá-las aqui.',
    exported: n => `${n} configurações salvas no arquivo (sem MQTT e celular, que pertencem a esta TV).`,
    searchHint: '/ ou Ctrl+K: buscar', theme: 'Claro ou escuro', guide: 'Guia', guideShow: 'Mostrar o guia',
    connection: 'Conexão', mqttBroker: 'Broker MQTT', noBroker: 'sem endereço do broker', entities: n => `${n} entidades`,
    lastPublish: s => `enviado ${ago(s, 'pt')}`, connectedFor: s => `conectado há ${ago(s, 'pt', true)}`, deviceId: 'ID do dispositivo',
    stateTopic: 'Estado', commandTopic: 'Comandos', devicesTitle: 'Dispositivos conectados', thisDevice: 'este dispositivo', remove: 'Remover', justNow: 'agora mesmo',
    fromSkin: 'Do tema', designTitle: 'Temas próprios', designSave: 'Salvar o design atual como tema', designName: 'Nome',
    designExport: 'Exportar (.json)', designImport: 'Importar', designDelete: n => `Excluir o tema ${n}?`, designFull: 'No máximo 10 temas próprios.',
    designBad: 'Este não é um arquivo de design do ShrugTV.', designNone: 'Nenhum tema próprio ainda.', designSaved: n => `Salvo como ${n}.`, designImported: n => `Importado como ${n}.`,
    deviceSeen: (seen, paired) => `último uso ${seen} · pareado ${paired}`, removeDevice: n => `Remover ${n}? Ele precisará do QR code ou do PIN de novo.`,
    noDevices: 'Nenhum dispositivo lembrado ainda.', rememberOff: '"Lembrar dispositivos conectados" está desativado: os dispositivos precisam de um código a cada início do app.', nameTaken: 'nome usado por outra TV, código adicionado', noTls: 'sem TLS', reachable: 'acessível',
    mqttStates: { connected: 'conectado', connecting: 'conectando …', off: 'desligado', 'no broker address': 'sem endereço do broker' },
    remoteMode: 'Modo do controle', modePad: 'Touchpad', modeKeys: 'Teclas', padLabel: 'Touchpad: deslize para as setas, toque para OK',
    padHint: 'Deslizar = setas\nToque = OK · Dois dedos = Voltar', replay: '⟲ Replay', audioOnly: 'Só áudio',
    recent: 'Assistidos recentemente', replyTo: n => `Responder a ${n}`, replyCancel: 'Cancelar resposta', emotes: 'Emotes',
    noEmotes: 'Nenhum emote visto neste chat ainda. Eles aparecem aqui assim que alguém usar um.', quickAdd: 'Adicionar uma frase rápida',
    quickNew: 'Nova frase rápida (máx. 60 caracteres)', quickRemove: t => `Remover "${t}" das frases rápidas?`,
    chatHint: 'Toque numa mensagem para responder com @nome. Segure uma frase rápida para removê-la.',
    tabUpdate: 'Versão', tabUpdateShort: 'Info', verInstalled: 'Instalada', verEdition: 'Edição', verEditionPublic: 'Pública', verEditionPrivate: 'Privada',
    verDevice: 'TV', verAndroid: 'Android', verLastCheck: 'Última verificação', verNever: 'ainda não', verCurrent: 'Você tem a versão mais recente.',
    verUnknown: 'Ainda não verificado.', verUnsupported: 'Esta edição não é atualizada pelo app, e aqui não há verificação de novas versões.',
    verAvailable: v => `A versão ${v} está disponível`, verCheck: 'Verificar agora', verChecking: 'Verificando …', verInstall: 'Instalar atualização',
    verInstallConfirm: v => `Baixar a versão ${v} e iniciar a instalação na TV? A TV pede que você confirme.`,
    verRelease: 'Página da versão', verInstallOff: 'A instalação pelo app está desativada. Ative nas configurações da TV: App → Instalar atualizações no app.',
    verAutoOff: 'A verificação diária está desativada (Configurações → Sobre → Verificar atualizações); "Verificar agora" funciona mesmo assim.',
    verDownloading: p => `Baixando … ${p} %`, verVerifying: 'Verificado. Confirme a instalação na TV com OK.',
    verErrors: { disabled: 'A instalação pelo app está desativada.', none: 'Nenhuma versão mais nova conhecida. Verifique primeiro.', need_permission: 'Permita "Instalar apps desconhecidos" para o ShrugTV na TV e tente de novo.',
      permission_unavailable: 'Esta TV não tem tela para "Instalar apps desconhecidos".', no_file: 'A versão não tem nenhum arquivo instalável.', bad_checksum: 'A soma de verificação não confere, nada foi instalado.',
      download_failed: 'O download falhou.' },
    verNotes: 'Novidades',
    favAdd: 'Marcar como favorito', favRemove: 'Remover dos favoritos', favFailed: 'Não foi possível salvar a estrela.',
    listsLabel: 'Listas do seletor de canais', listFollowed: 'Seguidos', listSameGame: 'Mesma categoria', listSameGameLanguage: 'Mesma categoria, meu idioma',
    listHistory: 'Assistidos recentemente', listTopStreams: 'Top transmissões', listTopLanguage: 'Top no meu idioma',
    listEmpty: 'Nada nesta lista agora.', listNeedsPlaying: 'Esta lista precisa de uma transmissão em andamento: ela mostra a categoria do canal que está passando.',
    homeTitle: 'Na tela inicial', homeHint: 'Abra esta página como ícone de app:',
    homeIos: 'iPhone: Safari → Compartilhar → Adicionar à Tela de Início.', homeAndroid: 'Android: menu do Chrome ⋮ → Adicionar à tela inicial.'
  },
  ru: {
    pairText: 'Отсканируйте QR-код на телевизоре или введите показанный там PIN.', pinLabel: 'PIN', connect: 'Подключиться',
    pinWrong: 'PIN неверный или истёк. Покажите на телевизоре новый код.', expired: 'Этот код истёк. Покажите на телевизоре новый.',
    offline: 'Телевизор недоступен. ShrugTV открыт?', tabRemote: 'Пульт', tabChat: 'Чат', tabFind: 'Поиск', tabPlayer: 'Плеер',
    up: 'Вверх', down: 'Вниз', left: 'Влево', right: 'Вправо', back: 'Назад', more: 'Ещё', playPause: 'Пуск/Пауза',
    prev: '◀ Канал', live: 'Эфир', next: 'Канал ▶', volume: 'Громкость трансляции', message: 'Сообщение', send: 'Отправить',
    chatOff: 'Написание в чат с телефона отключено в настройках телевизора.', search: 'Поиск', liveNow: 'Сейчас в эфире',
    results: 'Результаты', quality: 'Качество', chatLayout: 'Расположение чата', layoutSide: 'Рядом с видео',
    layoutOverlay: 'Поверх видео', layoutTransparent: 'Прозрачный', chatOn: 'Чат вкл.', chatOffBtn: 'Чат выкл.',
    stats: 'Статистика', close: 'Закрыть плеер', openTwitch: 'Открыть на Twitch', nothing: 'Ничего не воспроизводится',
    viewers: n => `${n.toLocaleString()} зрителей`, since: m => `в эфире ${Math.floor(m / 60)}:${String(m % 60).padStart(2, '0')} ч`,
    behind: s => `отставание ${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`, removed: 'сообщение удалено', offlineCh: 'не в эфире',
    tabSettings: 'Настройки', tabRemoteShort: 'Пульт', tabChatShort: 'Чат', tabFindShort: 'Поиск', tabPlayerShort: 'Плеер', tabSettingsShort: 'Настройки',
    searchChannels: 'Название канала', tabLog: 'Журнал',
    logOff: 'Журнал в браузере отключён на телевизоре (Настройки → Телефон и браузер → Журнал в браузере).',
    logAbout: 'Собственный журнал ShrugTV на этом телевизоре в реальном времени. Токены и пароли всегда удаляются. Нажмите на строку, чтобы увидеть подробности.',
    logSearch: 'Поиск по журналу', logSource: 'Источник', logAllSources: 'Все источники', logLevel: 'Уровень',
    logAll: 'Все', logErrors: 'Ошибки', logWarnings: 'Предупреждения', logInfo: 'Информация', logDebug: 'Отладка',
    logPause: 'Пауза', logResume: 'Продолжить', logCopy: 'Копировать', logExport: 'Сохранить .txt', logPrivate: 'Скрыть личные данные',
    logLatest: 'К последним ↓', logCopied: n => `Скопировано строк: ${n}`, logSaved: n => `Сохранено строк: ${n}`,
    logShown: (n, total) => `${n} из ${total} строк`, logEmpty: 'Нет подходящих строк.', logPaused: 'пауза',
    logTime: 'Время', logTag: 'Источник', logThread: 'Процесс/поток', logMessage: 'Сообщение', settingsOff: 'Изменение настроек из браузера отключено на телевизоре (Настройки → Телефон и браузер).',
    settingsSearch: 'Поиск по настройкам', export: 'Сохранить в файл', import: 'Загрузить из файла', saved: 'Сохранено', notSaved: 'Не сохранено: недопустимое значение',
    imported: (n, m) => `Загружено настроек: ${n}` + (m ? `, пропущено: ${m}` : ''), importBad: 'Это не файл настроек ShrugTV.',
    moveUp: 'Выше', run: 'Выполнить', done: 'Готово', tvOnly: 'Только на телевизоре', noScreen: 'Сначала откройте ShrugTV на телевизоре.', noResults: 'Настройки не найдены.',
    secretSet: 'сохранено – введите, чтобы заменить', secretEmpty: 'не задано', on: 'Вкл.', off: 'Выкл.', status: 'Состояние',
    pinned: 'Закреплённые', pin: 'Закрепить на боковой панели', unpin: 'Открепить', noPins: 'Закрепляйте настройки значком ☆, чтобы находить их здесь.',
    exported: n => `Настроек сохранено в файл: ${n} (без MQTT и компаньона, они относятся к этому телевизору).`,
    searchHint: '/ или Ctrl+K: поиск', theme: 'Светлая или тёмная тема', guide: 'Справка', guideShow: 'Показать справку',
    connection: 'Подключение', mqttBroker: 'MQTT-брокер', noBroker: 'нет адреса брокера', entities: n => `сущностей: ${n}`,
    lastPublish: s => `отправлено ${ago(s, 'ru')}`, connectedFor: s => `подключено ${ago(s, 'ru', true)}`, deviceId: 'ID устройства',
    stateTopic: 'Состояние', commandTopic: 'Команды', devicesTitle: 'Подключённые устройства', thisDevice: 'это устройство', remove: 'Удалить', justNow: 'только что',
    fromSkin: 'Из темы', designTitle: 'Свои темы', designSave: 'Сохранить текущее оформление как тему', designName: 'Название',
    designExport: 'Экспорт (.json)', designImport: 'Импорт', designDelete: n => `Удалить тему ${n}?`, designFull: 'Не более 10 своих тем.',
    designBad: 'Это не файл оформления ShrugTV.', designNone: 'Своих тем пока нет.', designSaved: n => `Сохранено как ${n}.`, designImported: n => `Импортировано как ${n}.`,
    deviceSeen: (seen, paired) => `использовалось: ${seen} · сопряжено: ${paired}`, removeDevice: n => `Удалить ${n}? Ему снова понадобится QR-код или PIN.`,
    noDevices: 'Пока нет запомненных устройств.', rememberOff: '«Запоминать подключённые устройства» выключено: после каждого запуска приложения устройствам нужен код.', nameTaken: 'имя занято другим телевизором, добавлен код', noTls: 'без TLS', reachable: 'доступно',
    mqttStates: { connected: 'подключено', connecting: 'подключение …', off: 'выкл.', 'no broker address': 'нет адреса брокера' },
    remoteMode: 'Режим пульта', modePad: 'Тачпад', modeKeys: 'Кнопки', padLabel: 'Тачпад: проведите пальцем для стрелок, коснитесь для OK',
    padHint: 'Свайп = стрелки\nКасание = OK · Два пальца = Назад', replay: '⟲ Повтор', audioOnly: 'Только звук',
    recent: 'Недавно смотрели', replyTo: n => `Ответить ${n}`, replyCancel: 'Отменить ответ', emotes: 'Смайлы',
    noEmotes: 'В этом чате пока не было смайлов. Они появятся здесь, как только кто-нибудь отправит один.', quickAdd: 'Добавить быструю фразу',
    quickNew: 'Новая быстрая фраза (до 60 символов)', quickRemove: t => `Удалить «${t}» из быстрых фраз?`,
    chatHint: 'Коснитесь сообщения, чтобы ответить через @имя. Удерживайте быструю фразу, чтобы удалить её.',
    tabUpdate: 'Версия', tabUpdateShort: 'Инфо', verInstalled: 'Установлена', verEdition: 'Выпуск', verEditionPublic: 'Публичный', verEditionPrivate: 'Личный',
    verDevice: 'Телевизор', verAndroid: 'Android', verLastCheck: 'Последняя проверка', verNever: 'ещё не было', verCurrent: 'У вас самая новая версия.',
    verUnknown: 'Ещё не проверялось.', verUnsupported: 'Этот выпуск не обновляется из приложения, и проверки новых версий здесь нет.',
    verAvailable: v => `Доступна версия ${v}`, verCheck: 'Проверить сейчас', verChecking: 'Проверка …', verInstall: 'Установить обновление',
    verInstallConfirm: v => `Скачать версию ${v} и начать установку на телевизоре? Телевизор попросит подтвердить.`,
    verRelease: 'Страница выпуска', verInstallOff: 'Установка из приложения отключена. Включите её в настройках телевизора: Приложение → Устанавливать обновления из приложения.',
    verAutoOff: 'Ежедневная проверка отключена (Настройки → О приложении → Проверять обновления); «Проверить сейчас» всё равно работает.',
    verDownloading: p => `Загрузка … ${p} %`, verVerifying: 'Проверено. Подтвердите установку на телевизоре кнопкой OK.',
    verErrors: { disabled: 'Установка из приложения отключена.', none: 'Более новая версия неизвестна. Сначала выполните проверку.', need_permission: 'Разрешите ShrugTV «Установку неизвестных приложений» на телевизоре и повторите попытку.',
      permission_unavailable: 'На этом телевизоре нет экрана «Установка неизвестных приложений».', no_file: 'В выпуске нет файла для установки.', bad_checksum: 'Контрольная сумма не совпадает, ничего не установлено.',
      download_failed: 'Не удалось скачать.' },
    verNotes: 'Что нового',
    favAdd: 'Добавить в избранное', favRemove: 'Убрать из избранного', favFailed: 'Не удалось сохранить звёздочку.',
    listsLabel: 'Списки переключателя каналов', listFollowed: 'Подписки', listSameGame: 'Та же категория', listSameGameLanguage: 'Та же категория, мой язык',
    listHistory: 'Недавно смотрели', listTopStreams: 'Популярные трансляции', listTopLanguage: 'Популярное на моём языке',
    listEmpty: 'Сейчас в этом списке ничего нет.', listNeedsPlaying: 'Для этого списка нужна идущая трансляция: он показывает категорию воспроизводимого канала.',
    homeTitle: 'На главном экране', homeHint: 'Откройте эту страницу как значок приложения:',
    homeIos: 'iPhone: Safari → Поделиться → На экран «Домой».', homeAndroid: 'Android: меню Chrome ⋮ → Добавить на главный экран.'
  },
  tr: {
    pairText: 'TV\'deki QR kodunu tarayın veya orada gösterilen PIN\'i girin.', pinLabel: 'PIN', connect: 'Bağlan',
    pinWrong: 'PIN yanlış veya süresi dolmuş. TV\'de yeni bir kod gösterin.', expired: 'Bu kodun süresi doldu. TV\'de yeni bir kod gösterin.',
    offline: 'TV\'ye ulaşılamıyor. ShrugTV açık mı?', tabRemote: 'Kumanda', tabChat: 'Sohbet', tabFind: 'Bul', tabPlayer: 'Oynatıcı',
    up: 'Yukarı', down: 'Aşağı', left: 'Sol', right: 'Sağ', back: 'Geri', more: 'Daha fazla', playPause: 'Oynat/Duraklat',
    prev: '◀ Kanal', live: 'Canlı', next: 'Kanal ▶', volume: 'Yayın sesi', message: 'Mesaj', send: 'Gönder',
    chatOff: 'Telefondan sohbete yazma TV ayarlarında kapalı.', search: 'Ara', liveNow: 'Şimdi canlı',
    results: 'Sonuçlar', quality: 'Kalite', chatLayout: 'Sohbet düzeni', layoutSide: 'Videonun yanında',
    layoutOverlay: 'Videonun üzerinde', layoutTransparent: 'Saydam', chatOn: 'Sohbet açık', chatOffBtn: 'Sohbet kapalı',
    stats: 'İstatistikler', close: 'Oynatıcıyı kapat', openTwitch: 'Twitch\'te aç', nothing: 'Oynatılan bir şey yok',
    viewers: n => `${n.toLocaleString()} izleyici`, since: m => `${Math.floor(m / 60)}:${String(m % 60).padStart(2, '0')} sa canlı`,
    behind: s => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')} geride`, removed: 'mesaj kaldırıldı', offlineCh: 'çevrimdışı',
    tabSettings: 'Ayarlar', tabRemoteShort: 'Kumanda', tabChatShort: 'Sohbet', tabFindShort: 'Bul', tabPlayerShort: 'Oynatıcı', tabSettingsShort: 'Ayarlar',
    searchChannels: 'Kanal adı', tabLog: 'Günlük',
    logOff: 'Tarayıcıdaki günlük TV\'de kapalı (Ayarlar → Telefon ve tarayıcı → Tarayıcıda günlük).',
    logAbout: "ShrugTV'nin bu TV'deki kendi günlüğü, canlı. Belirteçler ve parolalar her zaman kaldırılır. Ayrıntı için bir satıra tıklayın.",
    logSearch: 'Günlükte ara', logSource: 'Kaynak', logAllSources: 'Tüm kaynaklar', logLevel: 'Düzey',
    logAll: 'Tümü', logErrors: 'Hatalar', logWarnings: 'Uyarılar', logInfo: 'Bilgi', logDebug: 'Hata ayıklama',
    logPause: 'Duraklat', logResume: 'Devam', logCopy: 'Kopyala', logExport: '.txt olarak kaydet', logPrivate: 'Özel ayrıntıları gizle',
    logLatest: 'En sona atla ↓', logCopied: n => `${n} satır kopyalandı`, logSaved: n => `${n} satır kaydedildi`,
    logShown: (n, total) => `${total} satırdan ${n}`, logEmpty: 'Eşleşen satır yok.', logPaused: 'duraklatıldı',
    logTime: 'Zaman', logTag: 'Kaynak', logThread: 'İşlem/iş parçacığı', logMessage: 'Mesaj', settingsOff: 'Tarayıcıdan ayar değiştirme TV\'de kapalı (Ayarlar → Telefon ve tarayıcı).',
    settingsSearch: 'Ayarlarda ara', export: 'Dosyaya kaydet', import: 'Dosyadan yükle', saved: 'Kaydedildi', notSaved: 'Kaydedilmedi: değere izin verilmiyor',
    imported: (n, m) => `${n} ayar yüklendi` + (m ? `, ${m} atlandı` : ''), importBad: 'Bu bir ShrugTV ayar dosyası değil.',
    moveUp: 'Yukarı taşı', run: 'Çalıştır', done: 'Bitti', tvOnly: 'Yalnızca TV\'de', noScreen: 'Önce TV\'de ShrugTV\'yi açın.', noResults: 'Ayar bulunamadı.',
    secretSet: 'saklandı – değiştirmek için yazın', secretEmpty: 'ayarlanmadı', on: 'Açık', off: 'Kapalı', status: 'Durum',
    pinned: 'Sabitlenenler', pin: 'Kenar çubuğuna sabitle', unpin: 'Sabitlemeyi kaldır', noPins: 'Ayarları ☆ ile sabitleyin, burada bulun.',
    exported: n => `${n} ayar dosyaya kaydedildi (MQTT ve companion hariç, onlar bu TV'ye aittir).`,
    searchHint: '/ veya Ctrl+K: ara', theme: 'Açık veya koyu', guide: 'Rehber', guideShow: 'Rehberi göster',
    connection: 'Bağlantı', mqttBroker: 'MQTT aracısı', noBroker: 'aracı adresi yok', entities: n => `${n} varlık`,
    lastPublish: s => `${ago(s, 'tr')} gönderildi`, connectedFor: s => `${ago(s, 'tr', true)} süredir bağlı`, deviceId: 'Cihaz kimliği',
    stateTopic: 'Durum', commandTopic: 'Komutlar', devicesTitle: 'Bağlı cihazlar', thisDevice: 'bu cihaz', remove: 'Kaldır', justNow: 'az önce',
    fromSkin: 'Temadan', designTitle: 'Kendi temalarım', designSave: 'Mevcut tasarımı tema olarak kaydet', designName: 'Ad',
    designExport: 'Dışa aktar (.json)', designImport: 'İçe aktar', designDelete: n => `${n} teması silinsin mi?`, designFull: 'En fazla 10 kendi tema.',
    designBad: 'Bu bir ShrugTV tasarım dosyası değil.', designNone: 'Henüz kendi temanız yok.', designSaved: n => `${n} olarak kaydedildi.`, designImported: n => `${n} olarak içe aktarıldı.`,
    deviceSeen: (seen, paired) => `son kullanım ${seen} · eşleşme ${paired}`, removeDevice: n => `${n} kaldırılsın mı? Tekrar QR kodu veya PIN gerekecek.`,
    noDevices: 'Henüz hatırlanan cihaz yok.', rememberOff: '"Bağlı cihazları hatırla" kapalı: cihazlar her uygulama başlangıcından sonra kod ister.', nameTaken: 'ad başka bir TV tarafından kullanılıyor, kod eklendi', noTls: 'TLS olmadan', reachable: 'erişilebilir',
    mqttStates: { connected: 'bağlı', connecting: 'bağlanıyor …', off: 'kapalı', 'no broker address': 'aracı adresi yok' },
    remoteMode: 'Kumanda modu', modePad: 'Dokunmatik yüzey', modeKeys: 'Tuşlar', padLabel: 'Dokunmatik yüzey: oklar için kaydırın, Tamam için dokunun',
    padHint: 'Kaydır = ok tuşları\nDokun = Tamam · İki parmak = Geri', replay: '⟲ Tekrar', audioOnly: 'Yalnızca ses',
    recent: 'Son izlenenler', replyTo: n => `${n} kişisine yanıt ver`, replyCancel: 'Yanıtı iptal et', emotes: 'Emojiler',
    noEmotes: 'Bu sohbette henüz emoji görülmedi. Biri kullanır kullanmaz burada görünür.', quickAdd: 'Hızlı ifade ekle',
    quickNew: 'Yeni hızlı ifade (en fazla 60 karakter)', quickRemove: t => `"${t}" hızlı ifadelerden kaldırılsın mı?`,
    chatHint: 'Bir mesaja dokunarak @ad ile yanıtlayın. Kaldırmak için bir hızlı ifadeye basılı tutun.',
    tabUpdate: 'Sürüm', tabUpdateShort: 'Bilgi', verInstalled: 'Yüklü', verEdition: 'Sürüm türü', verEditionPublic: 'Herkese açık', verEditionPrivate: 'Özel',
    verDevice: 'TV', verAndroid: 'Android', verLastCheck: 'Son kontrol', verNever: 'henüz yok', verCurrent: 'En yeni sürüme sahipsiniz.',
    verUnknown: 'Henüz kontrol edilmedi.', verUnsupported: 'Bu sürüm türü uygulamadan güncellenmez ve burada yeni sürüm kontrolü yoktur.',
    verAvailable: v => `${v} sürümü hazır`, verCheck: 'Şimdi kontrol et', verChecking: 'Kontrol ediliyor …', verInstall: 'Güncellemeyi yükle',
    verInstallConfirm: v => `${v} sürümü indirilip TV\'de kurulum başlatılsın mı? TV sizden onay ister.`,
    verRelease: 'Sürüm sayfası', verInstallOff: "Uygulamadan kurulum kapalı. TV ayarlarından açın: Uygulama → Güncellemeleri uygulamadan yükle.",
    verAutoOff: 'Günlük kontrol kapalı (Ayarlar → Hakkında → Güncellemeleri denetle); "Şimdi kontrol et" yine de çalışır.',
    verDownloading: p => `İndiriliyor … %${p}`, verVerifying: "Doğrulandı. Kurulumu TV'de Tamam ile onaylayın.",
    verErrors: { disabled: 'Uygulamadan kurulum kapalı.', none: 'Bilinen daha yeni sürüm yok. Önce kontrol edin.', need_permission: 'TV\'de ShrugTV için "Bilinmeyen uygulamaları yükle" iznini verin, sonra tekrar deneyin.',
      permission_unavailable: 'Bu TV\'de "Bilinmeyen uygulamaları yükle" ekranı yok.', no_file: 'Sürümde yüklenebilir dosya yok.', bad_checksum: 'Sağlama toplamı uyuşmuyor, hiçbir şey yüklenmedi.',
      download_failed: 'İndirme başarısız oldu.' },
    verNotes: 'Yenilikler',
    favAdd: 'Favori olarak işaretle', favRemove: 'Favorilerden kaldır', favFailed: 'Yıldız kaydedilemedi.',
    listsLabel: 'Kanal değiştiricinin listeleri', listFollowed: 'Takip edilenler', listSameGame: 'Aynı kategori', listSameGameLanguage: 'Aynı kategori, benim dilim',
    listHistory: 'Son izlenenler', listTopStreams: 'En çok izlenen yayınlar', listTopLanguage: 'Dilimde en çok izlenenler',
    listEmpty: 'Bu listede şu an bir şey yok.', listNeedsPlaying: 'Bu liste çalışan bir yayın gerektirir: oynatılan kanalın kategorisini gösterir.',
    homeTitle: 'Ana ekranda', homeHint: 'Bu sayfayı uygulama simgesi olarak açın:',
    homeIos: 'iPhone: Safari → Paylaş → Ana Ekrana Ekle.', homeAndroid: 'Android: Chrome menüsü ⋮ → Ana ekrana ekle.'
  },
  it: {
    pairText: 'Inquadra il codice QR sul TV oppure inserisci il PIN mostrato lì.', pinLabel: 'PIN', connect: 'Connetti',
    pinWrong: 'PIN errato o scaduto. Mostra un nuovo codice sul TV.', expired: 'Questo codice è scaduto. Mostra un nuovo codice sul TV.',
    offline: 'TV non raggiungibile. ShrugTV è aperto?', tabRemote: 'Telecomando', tabChat: 'Chat', tabFind: 'Cerca', tabPlayer: 'Player',
    up: 'Su', down: 'Giù', left: 'Sinistra', right: 'Destra', back: 'Indietro', more: 'Altro', playPause: 'Riproduci/Pausa',
    prev: '◀ Canale', live: 'Diretta', next: 'Canale ▶', volume: 'Volume dello stream', message: 'Messaggio', send: 'Invia',
    chatOff: 'Scrivere in chat dal telefono è disattivato nelle impostazioni del TV.', search: 'Cerca', liveNow: 'In diretta ora',
    results: 'Risultati', quality: 'Qualità', chatLayout: 'Layout della chat', layoutSide: 'Accanto al video',
    layoutOverlay: 'Sopra il video', layoutTransparent: 'Trasparente', chatOn: 'Chat attiva', chatOffBtn: 'Chat disattivata',
    stats: 'Statistiche', close: 'Chiudi il player', openTwitch: 'Apri su Twitch', nothing: 'Nessuna riproduzione',
    viewers: n => `${n.toLocaleString()} spettatori`, since: m => `in diretta da ${Math.floor(m / 60)}:${String(m % 60).padStart(2, '0')} h`,
    behind: s => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')} di ritardo`, removed: 'messaggio rimosso', offlineCh: 'offline',
    tabSettings: 'Impostazioni', tabRemoteShort: 'Telecomando', tabChatShort: 'Chat', tabFindShort: 'Cerca', tabPlayerShort: 'Player', tabSettingsShort: 'Impostazioni',
    searchChannels: 'Nome del canale', tabLog: 'Log',
    logOff: 'Il log nel browser è disattivato sul TV (Impostazioni → Telefono e browser → Log nel browser).',
    logAbout: "Il log di ShrugTV su questo TV, in tempo reale. Token e password vengono sempre rimossi. Clicca una riga per i dettagli.",
    logSearch: 'Cerca nel log', logSource: 'Origine', logAllSources: 'Tutte le origini', logLevel: 'Livello',
    logAll: 'Tutti', logErrors: 'Errori', logWarnings: 'Avvisi', logInfo: 'Info', logDebug: 'Debug',
    logPause: 'Pausa', logResume: 'Riprendi', logCopy: 'Copia', logExport: 'Salva .txt', logPrivate: 'Nascondi i dettagli privati',
    logLatest: 'Vai all\'ultima ↓', logCopied: n => `${n} righe copiate`, logSaved: n => `${n} righe salvate`,
    logShown: (n, total) => `${n} di ${total} righe`, logEmpty: 'Nessuna riga corrispondente.', logPaused: 'in pausa',
    logTime: 'Ora', logTag: 'Origine', logThread: 'Processo/thread', logMessage: 'Messaggio', settingsOff: 'La modifica delle impostazioni dal browser è disattivata sul TV (Impostazioni → Telefono e browser).',
    settingsSearch: 'Cerca nelle impostazioni', export: 'Salva su file', import: 'Carica da file', saved: 'Salvato', notSaved: 'Non salvato: valore non consentito',
    imported: (n, m) => `${n} impostazioni caricate` + (m ? `, ${m} saltate` : ''), importBad: 'Questo non è un file di impostazioni di ShrugTV.',
    moveUp: 'Sposta su', run: 'Esegui', done: 'Fatto', tvOnly: 'Solo sul TV', noScreen: 'Apri prima ShrugTV sul TV.', noResults: 'Nessuna impostazione trovata.',
    secretSet: 'salvato – scrivi per sostituire', secretEmpty: 'non impostato', on: 'Attivo', off: 'Disattivato', status: 'Stato',
    pinned: 'Fissate', pin: 'Fissa nella barra laterale', unpin: 'Rimuovi dai fissati', noPins: 'Fissa le impostazioni con ☆ per trovarle qui.',
    exported: n => `${n} impostazioni salvate nel file (senza MQTT e telefono, che appartengono a questo TV).`,
    searchHint: '/ o Ctrl+K: cerca', theme: 'Chiaro o scuro', guide: 'Guida', guideShow: 'Mostra la guida',
    connection: 'Connessione', mqttBroker: 'Broker MQTT', noBroker: 'nessun indirizzo del broker', entities: n => `${n} entità`,
    lastPublish: s => `inviato ${ago(s, 'it')}`, connectedFor: s => `connesso da ${ago(s, 'it', true)}`, deviceId: 'ID del dispositivo',
    stateTopic: 'Stato', commandTopic: 'Comandi', devicesTitle: 'Dispositivi collegati', thisDevice: 'questo dispositivo', remove: 'Rimuovi', justNow: 'proprio ora',
    fromSkin: 'Dal tema', designTitle: 'Temi personali', designSave: 'Salva il design attuale come tema', designName: 'Nome',
    designExport: 'Esporta (.json)', designImport: 'Importa', designDelete: n => `Eliminare il tema ${n}?`, designFull: 'Al massimo 10 temi personali.',
    designBad: 'Questo non è un file di design di ShrugTV.', designNone: 'Ancora nessun tema personale.', designSaved: n => `Salvato come ${n}.`, designImported: n => `Importato come ${n}.`,
    deviceSeen: (seen, paired) => `ultimo utilizzo ${seen} · associato ${paired}`, removeDevice: n => `Rimuovere ${n}? Servirà di nuovo il codice QR o il PIN.`,
    noDevices: 'Ancora nessun dispositivo ricordato.', rememberOff: '"Ricorda i dispositivi collegati" è disattivato: i dispositivi richiedono un codice a ogni avvio dell\'app.', nameTaken: 'nome usato da un altro TV, codice aggiunto', noTls: 'senza TLS', reachable: 'raggiungibile',
    mqttStates: { connected: 'connesso', connecting: 'connessione …', off: 'disattivato', 'no broker address': 'nessun indirizzo del broker' },
    remoteMode: 'Modalità telecomando', modePad: 'Touchpad', modeKeys: 'Tasti', padLabel: 'Touchpad: scorri per le frecce, tocca per OK',
    padHint: 'Scorrere = frecce\nToccare = OK · Due dita = Indietro', replay: '⟲ Replay', audioOnly: 'Solo audio',
    recent: 'Visti di recente', replyTo: n => `Rispondi a ${n}`, replyCancel: 'Annulla risposta', emotes: 'Emote',
    noEmotes: 'Nessuna emote vista in questa chat finora. Compaiono qui non appena qualcuno ne usa una.', quickAdd: 'Aggiungi una frase rapida',
    quickNew: 'Nuova frase rapida (max. 60 caratteri)', quickRemove: t => `Rimuovere "${t}" dalle frasi rapide?`,
    chatHint: 'Tocca un messaggio per rispondere con @nome. Tieni premuta una frase rapida per rimuoverla.',
    tabUpdate: 'Versione', tabUpdateShort: 'Info', verInstalled: 'Installata', verEdition: 'Edizione', verEditionPublic: 'Pubblica', verEditionPrivate: 'Privata',
    verDevice: 'TV', verAndroid: 'Android', verLastCheck: 'Ultimo controllo', verNever: 'non ancora', verCurrent: 'Hai la versione più recente.',
    verUnknown: 'Non ancora controllato.', verUnsupported: "Questa edizione non si aggiorna dall'app e qui non c'è il controllo delle nuove versioni.",
    verAvailable: v => `È disponibile la versione ${v}`, verCheck: 'Controlla ora', verChecking: 'Controllo in corso …', verInstall: "Installa l'aggiornamento",
    verInstallConfirm: v => `Scaricare la versione ${v} e avviare l'installazione sul TV? Il TV ti chiede di confermare.`,
    verRelease: 'Pagina della versione', verInstallOff: "L'installazione dall'app è disattivata. Attivala nelle impostazioni del TV: App → Installa aggiornamenti nell'app.",
    verAutoOff: 'Il controllo giornaliero è disattivato (Impostazioni → Info → Cerca aggiornamenti); "Controlla ora" funziona comunque.',
    verDownloading: p => `Download in corso … ${p} %`, verVerifying: "Verificato. Conferma l'installazione sul TV con OK.",
    verErrors: { disabled: "L'installazione dall'app è disattivata.", none: 'Nessuna versione più recente nota. Controlla prima.', need_permission: 'Consenti "Installa app sconosciute" per ShrugTV sul TV, poi riprova.',
      permission_unavailable: 'Questo TV non ha una schermata per "Installa app sconosciute".', no_file: 'La versione non ha alcun file installabile.', bad_checksum: 'Il checksum non corrisponde, non è stato installato nulla.',
      download_failed: 'Il download non è riuscito.' },
    verNotes: 'Novità',
    favAdd: 'Segna come preferito', favRemove: 'Rimuovi dai preferiti', favFailed: 'Non è stato possibile salvare la stella.',
    listsLabel: 'Liste del cambio canale', listFollowed: 'Seguiti', listSameGame: 'Stessa categoria', listSameGameLanguage: 'Stessa categoria, nella mia lingua',
    listHistory: 'Visti di recente', listTopStreams: 'Stream più visti', listTopLanguage: 'Più visti nella mia lingua',
    listEmpty: "In questo momento non c'è nulla in questa lista.", listNeedsPlaying: 'Questa lista richiede uno stream in riproduzione: mostra la categoria del canale in corso.',
    homeTitle: 'Nella schermata Home', homeHint: 'Apri questa pagina come icona di app:',
    homeIos: 'iPhone: Safari → Condividi → Aggiungi alla schermata Home.', homeAndroid: 'Android: menu di Chrome ⋮ → Aggiungi a schermata Home.'
  },
  ja: {
    pairText: 'テレビのQRコードを読み取るか、表示されているPINを入力してください。', pinLabel: 'PIN', connect: '接続',
    pinWrong: 'PINが違うか期限切れです。テレビで新しいコードを表示してください。', expired: 'このコードは期限切れです。テレビで新しいコードを表示してください。',
    offline: 'テレビに接続できません。ShrugTVは起動していますか?', tabRemote: 'リモコン', tabChat: 'チャット', tabFind: '検索', tabPlayer: 'プレーヤー',
    up: '上', down: '下', left: '左', right: '右', back: '戻る', more: 'その他', playPause: '再生/一時停止',
    prev: '◀ チャンネル', live: 'ライブ', next: 'チャンネル ▶', volume: '配信の音量', message: 'メッセージ', send: '送信',
    chatOff: 'スマホからのチャット書き込みは、テレビの設定でオフになっています。', search: '検索', liveNow: '配信中',
    results: '結果', quality: '画質', chatLayout: 'チャットのレイアウト', layoutSide: '映像の横',
    layoutOverlay: '映像の上', layoutTransparent: '透明', chatOn: 'チャットオン', chatOffBtn: 'チャットオフ',
    stats: '統計', close: 'プレーヤーを閉じる', openTwitch: 'Twitchで開く', nothing: '再生中のものはありません',
    viewers: n => `視聴者 ${n.toLocaleString()} 人`, since: m => `配信 ${Math.floor(m / 60)}:${String(m % 60).padStart(2, '0')} 時間`,
    behind: s => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')} 遅れ`, removed: 'メッセージは削除されました', offlineCh: 'オフライン',
    tabSettings: '設定', tabRemoteShort: 'リモコン', tabChatShort: 'チャット', tabFindShort: '検索', tabPlayerShort: '再生', tabSettingsShort: '設定',
    searchChannels: 'チャンネル名', tabLog: 'ログ',
    logOff: 'ブラウザでのログ表示は、テレビでオフになっています(設定 → スマホとブラウザ → ブラウザでログを表示)。',
    logAbout: "このテレビのShrugTV自身のログをライブで表示します。トークンとパスワードは常に除去されます。行をクリックすると詳細を表示します。",
    logSearch: 'ログを検索', logSource: '発生源', logAllSources: 'すべての発生源', logLevel: 'レベル',
    logAll: 'すべて', logErrors: 'エラー', logWarnings: '警告', logInfo: '情報', logDebug: 'デバッグ',
    logPause: '一時停止', logResume: '再開', logCopy: 'コピー', logExport: '.txtで保存', logPrivate: '個人情報を隠す',
    logLatest: '最新へ移動 ↓', logCopied: n => `${n} 行をコピーしました`, logSaved: n => `${n} 行を保存しました`,
    logShown: (n, total) => `${total} 行中 ${n} 行`, logEmpty: '一致する行がありません。', logPaused: '一時停止中',
    logTime: '時刻', logTag: '発生源', logThread: 'プロセス/スレッド', logMessage: 'メッセージ', settingsOff: 'ブラウザからの設定変更は、テレビでオフになっています(設定 → スマホとブラウザ)。',
    settingsSearch: '設定を検索', export: 'ファイルに保存', import: 'ファイルから読み込み', saved: '保存しました', notSaved: '保存されませんでした: 値が許可されていません',
    imported: (n, m) => `${n} 件の設定を読み込みました` + (m ? `(${m} 件スキップ)` : ''), importBad: 'ShrugTVの設定ファイルではありません。',
    moveUp: '上へ', run: '実行', done: '完了', tvOnly: 'テレビでのみ', noScreen: '先にテレビでShrugTVを開いてください。', noResults: '設定が見つかりません。',
    secretSet: '保存済み - 入力すると置き換わります', secretEmpty: '未設定', on: 'オン', off: 'オフ', status: '状態',
    pinned: 'ピン留め', pin: 'サイドバーにピン留め', unpin: 'ピン留めを解除', noPins: '☆ で設定をピン留めすると、ここに表示されます。',
    exported: n => `${n} 件の設定をファイルに保存しました(MQTTとコンパニオンはこのテレビ専用のため含まれません)。`,
    searchHint: '/ または Ctrl+K: 検索', theme: 'ライト/ダーク', guide: 'ガイド', guideShow: 'ガイドを表示',
    connection: '接続', mqttBroker: 'MQTTブローカー', noBroker: 'ブローカーのアドレスなし', entities: n => `エンティティ ${n} 件`,
    lastPublish: s => `${ago(s, 'ja')}に送信`, connectedFor: s => `${ago(s, 'ja', true)}接続中`, deviceId: 'デバイスID',
    stateTopic: '状態', commandTopic: 'コマンド', devicesTitle: '接続済みデバイス', thisDevice: 'このデバイス', remove: '削除', justNow: 'たった今',
    fromSkin: 'スキンに従う', designTitle: 'オリジナルスキン', designSave: '現在のデザインをスキンとして保存', designName: '名前',
    designExport: 'エクスポート(.json)', designImport: 'インポート', designDelete: n => `スキン「${n}」を削除しますか?`, designFull: 'オリジナルスキンは最大10個までです。',
    designBad: 'ShrugTVのデザインファイルではありません。', designNone: 'オリジナルスキンはまだありません。', designSaved: n => `「${n}」として保存しました。`, designImported: n => `「${n}」としてインポートしました。`,
    deviceSeen: (seen, paired) => `最終利用 ${seen} · ペアリング ${paired}`, removeDevice: n => `${n}を削除しますか? 再びQRコードまたはPINが必要になります。`,
    noDevices: '記憶されているデバイスはまだありません。', rememberOff: '「接続したデバイスを記憶」がオフです。アプリの起動のたびにコードが必要です。', nameTaken: '他のテレビが使用中の名前のため、コードを追加しました', noTls: 'TLSなし', reachable: '到達可能',
    mqttStates: { connected: '接続済み', connecting: '接続中…', off: 'オフ', 'no broker address': 'ブローカーのアドレスなし' },
    remoteMode: 'リモコンモード', modePad: 'タッチパッド', modeKeys: 'キー', padLabel: 'タッチパッド: スワイプで矢印、タップでOK',
    padHint: 'スワイプ = 矢印キー\nタップ = OK · 2本指 = 戻る', replay: '⟲ リプレイ', audioOnly: '音声のみ',
    recent: '最近見たもの', replyTo: n => `${n}に返信`, replyCancel: '返信をキャンセル', emotes: 'エモート',
    noEmotes: 'このチャットではまだエモートが見つかっていません。誰かが使うとすぐここに表示されます。', quickAdd: 'クイックフレーズを追加',
    quickNew: '新しいクイックフレーズ(60文字まで)', quickRemove: t => `「${t}」をクイックフレーズから削除しますか?`,
    chatHint: 'メッセージをタップすると @名前 で返信できます。クイックフレーズを長押しすると削除できます。',
    tabUpdate: 'バージョン', tabUpdateShort: '情報', verInstalled: 'インストール済み', verEdition: 'エディション', verEditionPublic: '公開版', verEditionPrivate: '非公開版',
    verDevice: 'テレビ', verAndroid: 'Android', verLastCheck: '最終確認', verNever: 'まだありません', verCurrent: '最新バージョンです。',
    verUnknown: 'まだ確認していません。', verUnsupported: 'このエディションはアプリからは更新されず、ここでは新バージョンの確認もできません。',
    verAvailable: v => `バージョン ${v} が利用できます`, verCheck: '今すぐ確認', verChecking: '確認中 …', verInstall: 'アップデートをインストール',
    verInstallConfirm: v => `バージョン ${v} をダウンロードして、テレビでインストールを開始しますか? テレビで確認が求められます。`,
    verRelease: 'リリースページ', verInstallOff: 'アプリからのインストールはオフです。テレビの設定でオンにしてください: アプリ → アプリからアップデートをインストール。',
    verAutoOff: '毎日の確認はオフです(設定 → 情報 → アップデートを確認)。「今すぐ確認」は使えます。',
    verDownloading: p => `ダウンロード中 … ${p} %`, verVerifying: '確認しました。テレビでOKを押してインストールを承認してください。',
    verErrors: { disabled: 'アプリからのインストールはオフです。', none: '新しいバージョンは見つかっていません。先に確認してください。', need_permission: 'テレビでShrugTVに「不明なアプリのインストール」を許可してから、もう一度お試しください。',
      permission_unavailable: 'このテレビには「不明なアプリのインストール」の画面がありません。', no_file: 'このリリースにはインストールできるファイルがありません。', bad_checksum: 'チェックサムが一致しないため、何もインストールされませんでした。',
      download_failed: 'ダウンロードに失敗しました。' },
    verNotes: '新機能',
    favAdd: 'お気に入りに追加', favRemove: 'お気に入りから削除', favFailed: '星を保存できませんでした。',
    listsLabel: 'チャンネル切り替えのリスト', listFollowed: 'フォロー中', listSameGame: '同じカテゴリ', listSameGameLanguage: '同じカテゴリ(自分の言語)',
    listHistory: '最近見たもの', listTopStreams: '人気の配信', listTopLanguage: '自分の言語の人気配信',
    listEmpty: '今このリストには何もありません。', listNeedsPlaying: 'このリストには再生中の配信が必要です。再生中のチャンネルのカテゴリを表示します。',
    homeTitle: 'ホーム画面に追加', homeHint: 'このページをアプリのアイコンとして開く:',
    homeIos: 'iPhone: Safari → 共有 → ホーム画面に追加。', homeAndroid: 'Android: Chromeのメニュー ⋮ → ホーム画面に追加。'
  },
  ko: {
    pairText: 'TV의 QR 코드를 스캔하거나 TV에 표시된 PIN을 입력하세요.', pinLabel: 'PIN', connect: '연결',
    pinWrong: 'PIN이 틀렸거나 만료됐어요. TV에서 새 코드를 표시하세요.', expired: '코드가 만료됐어요. TV에서 새 코드를 표시하세요.',
    offline: 'TV에 연결할 수 없어요. ShrugTV가 열려 있나요?', tabRemote: '리모컨', tabChat: '채팅', tabFind: '찾기', tabPlayer: '플레이어',
    up: '위', down: '아래', left: '왼쪽', right: '오른쪽', back: '뒤로', more: '더보기', playPause: '재생/일시정지',
    prev: '◀ 채널', live: '라이브', next: '채널 ▶', volume: '스트림 볼륨', message: '메시지', send: '보내기',
    chatOff: '휴대폰에서 채팅 쓰기가 TV 설정에서 꺼져 있어요.', search: '검색', liveNow: '지금 라이브',
    results: '결과', quality: '화질', chatLayout: '채팅 레이아웃', layoutSide: '영상 옆',
    layoutOverlay: '영상 위', layoutTransparent: '투명', chatOn: '채팅 켜기', chatOffBtn: '채팅 끄기',
    stats: '통계', close: '플레이어 닫기', openTwitch: 'Twitch에서 열기', nothing: '재생 중인 항목 없음',
    viewers: n => `시청자 ${n.toLocaleString()}명`, since: m => `${Math.floor(m / 60)}:${String(m % 60).padStart(2, '0')}시간째 방송 중`,
    behind: s => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')} 지연`, removed: '삭제된 메시지', offlineCh: '오프라인',
    tabSettings: '설정', tabRemoteShort: '리모컨', tabChatShort: '채팅', tabFindShort: '찾기', tabPlayerShort: '플레이어', tabSettingsShort: '설정',
    searchChannels: '채널 이름', tabLog: '로그',
    logOff: 'TV에서 브라우저 로그가 꺼져 있어요 (설정 → 휴대폰 및 브라우저 → 브라우저에서 로그 보기).',
    logAbout: "이 TV의 ShrugTV 자체 로그를 실시간으로 보여 줘요. 토큰과 비밀번호는 항상 제거돼요. 줄을 클릭하면 자세히 볼 수 있어요.",
    logSearch: '로그 검색', logSource: '출처', logAllSources: '모든 출처', logLevel: '수준',
    logAll: '전체', logErrors: '오류', logWarnings: '경고', logInfo: '정보', logDebug: '디버그',
    logPause: '일시정지', logResume: '계속', logCopy: '복사', logExport: '.txt로 저장', logPrivate: '개인 정보 숨기기',
    logLatest: '최신으로 이동 ↓', logCopied: n => `${n}줄 복사됨`, logSaved: n => `${n}줄 저장됨`,
    logShown: (n, total) => `${total}줄 중 ${n}줄`, logEmpty: '일치하는 줄이 없어요.', logPaused: '일시정지됨',
    logTime: '시간', logTag: '출처', logThread: '프로세스/스레드', logMessage: '메시지', settingsOff: '브라우저에서 설정 변경이 TV에서 꺼져 있어요 (설정 → 휴대폰 및 브라우저).',
    settingsSearch: '설정 검색', export: '파일로 저장', import: '파일에서 불러오기', saved: '저장됨', notSaved: '저장되지 않음: 허용되지 않는 값',
    imported: (n, m) => `설정 ${n}개를 불러왔어요` + (m ? `, ${m}개는 건너뜀` : ''), importBad: 'ShrugTV 설정 파일이 아니에요.',
    moveUp: '위로', run: '실행', done: '완료', tvOnly: 'TV에서만 가능', noScreen: '먼저 TV에서 ShrugTV를 여세요.', noResults: '설정을 찾을 수 없어요.',
    secretSet: '저장됨 – 입력하면 교체', secretEmpty: '설정되지 않음', on: '켜짐', off: '꺼짐', status: '상태',
    pinned: '고정됨', pin: '사이드바에 고정', unpin: '고정 해제', noPins: '☆로 설정을 고정하면 여기에서 찾을 수 있어요.',
    exported: n => `설정 ${n}개를 파일에 저장했어요 (MQTT와 연동 정보는 이 TV에 속하므로 제외).`,
    searchHint: '/ 또는 Ctrl+K: 검색', theme: '밝게 또는 어둡게', guide: '안내', guideShow: '안내 보기',
    connection: '연결', mqttBroker: 'MQTT 브로커', noBroker: '브로커 주소 없음', entities: n => `엔티티 ${n}개`,
    lastPublish: s => `${ago(s, 'ko')} 전송`, connectedFor: s => `${ago(s, 'ko', true)} 동안 연결됨`, deviceId: '기기 ID',
    stateTopic: '상태', commandTopic: '명령', devicesTitle: '연결된 기기', thisDevice: '이 기기', remove: '삭제', justNow: '방금',
    fromSkin: '스킨 기준', designTitle: '나만의 스킨', designSave: '현재 디자인을 스킨으로 저장', designName: '이름',
    designExport: '내보내기 (.json)', designImport: '가져오기', designDelete: n => `스킨 ${n}을(를) 삭제할까요?`, designFull: '나만의 스킨은 최대 10개예요.',
    designBad: 'ShrugTV 디자인 파일이 아니에요.', designNone: '나만의 스킨이 아직 없어요.', designSaved: n => `${n}(으)로 저장했어요.`, designImported: n => `${n}(으)로 가져왔어요.`,
    deviceSeen: (seen, paired) => `마지막 사용 ${seen} · 연결 ${paired}`, removeDevice: n => `${n}을(를) 삭제할까요? 다시 QR 코드나 PIN이 필요해요.`,
    noDevices: '기억된 기기가 아직 없어요.', rememberOff: '"연결된 기기 기억"이 꺼져 있어요: 앱을 시작할 때마다 기기에 코드가 필요해요.', nameTaken: '다른 TV가 사용 중인 이름이라 코드를 추가했어요', noTls: 'TLS 없음', reachable: '연결 가능',
    mqttStates: { connected: '연결됨', connecting: '연결 중 …', off: '꺼짐', 'no broker address': '브로커 주소 없음' },
    remoteMode: '리모컨 모드', modePad: '터치패드', modeKeys: '버튼', padLabel: '터치패드: 밀어서 방향키, 탭하면 확인',
    padHint: '밀기 = 방향키\n탭 = 확인 · 두 손가락 = 뒤로', replay: '⟲ 다시보기', audioOnly: '오디오만',
    recent: '최근 시청', replyTo: n => `${n}에게 답장`, replyCancel: '답장 취소', emotes: '이모티콘',
    noEmotes: '이 채팅에서는 아직 이모티콘이 보이지 않았어요. 누군가 사용하면 바로 여기에 나타나요.', quickAdd: '빠른 문구 추가',
    quickNew: '새 빠른 문구 (최대 60자)', quickRemove: t => `빠른 문구에서 "${t}"를 삭제할까요?`,
    chatHint: '메시지를 탭하면 @이름으로 답장할 수 있어요. 빠른 문구를 길게 누르면 삭제돼요.',
    tabUpdate: '버전', tabUpdateShort: '정보', verInstalled: '설치됨', verEdition: '에디션', verEditionPublic: '공개', verEditionPrivate: '비공개',
    verDevice: 'TV', verAndroid: 'Android', verLastCheck: '마지막 확인', verNever: '아직 없음', verCurrent: '최신 버전이에요.',
    verUnknown: '아직 확인하지 않았어요.', verUnsupported: '이 에디션은 앱에서 업데이트되지 않고, 여기서는 새 버전 확인도 없어요.',
    verAvailable: v => `버전 ${v}을(를) 사용할 수 있어요`, verCheck: '지금 확인', verChecking: '확인하는 중 …', verInstall: '업데이트 설치',
    verInstallConfirm: v => `버전 ${v}을(를) 내려받아 TV에서 설치를 시작할까요? TV에서 확인을 요청해요.`,
    verRelease: '릴리스 페이지', verInstallOff: '앱에서 설치가 꺼져 있어요. TV 설정에서 켜 주세요: 앱 → 앱에서 업데이트 설치.',
    verAutoOff: '매일 확인이 꺼져 있어요 (설정 → 정보 → 업데이트 확인). "지금 확인"은 그래도 작동해요.',
    verDownloading: p => `내려받는 중 … ${p} %`, verVerifying: '확인했어요. TV에서 확인 버튼으로 설치를 승인해 주세요.',
    verErrors: { disabled: '앱에서 설치가 꺼져 있어요.', none: '알려진 새 버전이 없어요. 먼저 확인해 주세요.', need_permission: 'TV에서 ShrugTV에 "출처를 알 수 없는 앱 설치"를 허용한 뒤 다시 시도해 주세요.',
      permission_unavailable: '이 TV에는 "출처를 알 수 없는 앱 설치" 화면이 없어요.', no_file: '이 릴리스에는 설치할 수 있는 파일이 없어요.', bad_checksum: '체크섬이 일치하지 않아 아무것도 설치되지 않았어요.',
      download_failed: '내려받기에 실패했어요.' },
    verNotes: '새로운 기능',
    favAdd: '즐겨찾기로 표시', favRemove: '즐겨찾기에서 삭제', favFailed: '별을 저장하지 못했어요.',
    listsLabel: '채널 전환 목록', listFollowed: '팔로우 중', listSameGame: '같은 카테고리', listSameGameLanguage: '같은 카테고리, 내 언어',
    listHistory: '최근 시청', listTopStreams: '인기 스트림', listTopLanguage: '내 언어 인기 스트림',
    listEmpty: '지금은 이 목록에 아무것도 없어요.', listNeedsPlaying: '이 목록은 재생 중인 스트림이 필요해요. 재생 중인 채널의 카테고리를 보여줘요.',
    homeTitle: '홈 화면에', homeHint: '이 페이지를 앱 아이콘으로 열어 보세요:',
    homeIos: 'iPhone: Safari → 공유 → 홈 화면에 추가.', homeAndroid: 'Android: Chrome 메뉴 ⋮ → 홈 화면에 추가.'
  },
  zh: {
    pairText: '请扫描电视上的二维码,或输入电视上显示的 PIN。', pinLabel: 'PIN', connect: '连接',
    pinWrong: 'PIN 错误或已过期。请在电视上显示新的验证码。', expired: '此验证码已过期。请在电视上显示新的验证码。',
    offline: '无法连接电视。ShrugTV 是否已打开?', tabRemote: '遥控', tabChat: '聊天', tabFind: '查找', tabPlayer: '播放器',
    up: '上', down: '下', left: '左', right: '右', back: '返回', more: '更多', playPause: '播放/暂停',
    prev: '◀ 频道', live: '直播', next: '频道 ▶', volume: '直播音量', message: '消息', send: '发送',
    chatOff: '电视设置中已关闭用手机在聊天中发言。', search: '搜索', liveNow: '正在直播',
    results: '结果', quality: '画质', chatLayout: '聊天布局', layoutSide: '视频旁边',
    layoutOverlay: '覆盖在视频上', layoutTransparent: '透明', chatOn: '聊天开', chatOffBtn: '聊天关',
    stats: '统计', close: '关闭播放器', openTwitch: '在 Twitch 打开', nothing: '没有正在播放的内容',
    viewers: n => `${n.toLocaleString()} 位观众`, since: m => `已直播 ${Math.floor(m / 60)}:${String(m % 60).padStart(2, '0')} 小时`,
    behind: s => `落后 ${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`, removed: '消息已移除', offlineCh: '离线',
    tabSettings: '设置', tabRemoteShort: '遥控', tabChatShort: '聊天', tabFindShort: '查找', tabPlayerShort: '播放器', tabSettingsShort: '设置',
    searchChannels: '频道名称', tabLog: '日志',
    logOff: '电视上已关闭浏览器中的日志(设置 → 手机和浏览器 → 浏览器中的日志)。',
    logAbout: "这台电视上 ShrugTV 自己的日志,实时显示。令牌和密码一律会被移除。点击一行查看详情。",
    logSearch: '搜索日志', logSource: '来源', logAllSources: '所有来源', logLevel: '级别',
    logAll: '全部', logErrors: '错误', logWarnings: '警告', logInfo: '信息', logDebug: '调试',
    logPause: '暂停', logResume: '继续', logCopy: '复制', logExport: '保存 .txt', logPrivate: '隐藏私密细节',
    logLatest: '跳到最新 ↓', logCopied: n => `已复制 ${n} 行`, logSaved: n => `已保存 ${n} 行`,
    logShown: (n, total) => `${n} / ${total} 行`, logEmpty: '没有匹配的行。', logPaused: '已暂停',
    logTime: '时间', logTag: '来源', logThread: '进程/线程', logMessage: '消息', settingsOff: '电视上已关闭从浏览器更改设置(设置 → 手机和浏览器)。',
    settingsSearch: '搜索设置', export: '保存到文件', import: '从文件加载', saved: '已保存', notSaved: '未保存:不允许该值',
    imported: (n, m) => `已加载 ${n} 项设置` + (m ? `,跳过 ${m} 项` : ''), importBad: '这不是 ShrugTV 设置文件。',
    moveUp: '上移', run: '运行', done: '完成', tvOnly: '仅限电视', noScreen: '请先在电视上打开 ShrugTV。', noResults: '未找到设置。',
    secretSet: '已保存 – 输入可替换', secretEmpty: '未设置', on: '开', off: '关', status: '状态',
    pinned: '已固定', pin: '固定到侧边栏', unpin: '取消固定', noPins: '用 ☆ 固定设置,即可在这里找到它们。',
    exported: n => `已将 ${n} 项设置保存到文件(不含 MQTT 和手机连接,它们属于这台电视)。`,
    searchHint: '/ 或 Ctrl+K:搜索', theme: '浅色或深色', guide: '指南', guideShow: '显示指南',
    connection: '连接', mqttBroker: 'MQTT 代理', noBroker: '无代理地址', entities: n => `${n} 个实体`,
    lastPublish: s => `${ago(s, 'zh')}发送`, connectedFor: s => `已连接 ${ago(s, 'zh', true)}`, deviceId: '设备 ID',
    stateTopic: '状态', commandTopic: '命令', devicesTitle: '已连接设备', thisDevice: '这台设备', remove: '移除', justNow: '刚刚',
    fromSkin: '来自主题', designTitle: '自定义主题', designSave: '将当前设计保存为主题', designName: '名称',
    designExport: '导出 (.json)', designImport: '导入', designDelete: n => `删除主题 ${n}?`, designFull: '最多 10 个自定义主题。',
    designBad: '这不是 ShrugTV 设计文件。', designNone: '还没有自定义主题。', designSaved: n => `已保存为 ${n}。`, designImported: n => `已导入为 ${n}。`,
    deviceSeen: (seen, paired) => `上次使用 ${seen} · 配对于 ${paired}`, removeDevice: n => `移除 ${n}?之后需要重新使用二维码或 PIN。`,
    noDevices: '尚未记住任何设备。', rememberOff: '“记住已连接的设备”已关闭:每次应用启动后设备都需要验证码。', nameTaken: '名称已被另一台电视使用,已加短码', noTls: '未使用 TLS', reachable: '可连接',
    mqttStates: { connected: '已连接', connecting: '连接中 …', off: '关', 'no broker address': '无代理地址' },
    remoteMode: '遥控模式', modePad: '触控板', modeKeys: '按键', padLabel: '触控板:滑动为方向键,轻点为确定',
    padHint: '滑动 = 方向键\n轻点 = 确定 · 双指 = 返回', replay: '⟲ 回放', audioOnly: '仅音频',
    recent: '最近观看', replyTo: n => `回复 ${n}`, replyCancel: '取消回复', emotes: '表情',
    noEmotes: '这个聊天里还没有出现过表情。有人使用后会立即显示在这里。', quickAdd: '添加快捷短语',
    quickNew: '新快捷短语(最多 60 个字符)', quickRemove: t => `从快捷短语中移除“${t}”?`,
    chatHint: '轻点消息即可用 @名称 回复。长按快捷短语可将其移除。',
    tabUpdate: '版本', tabUpdateShort: '信息', verInstalled: '已安装', verEdition: '版本类型', verEditionPublic: '公开版', verEditionPrivate: '私有版',
    verDevice: '电视', verAndroid: 'Android', verLastCheck: '上次检查', verNever: '尚未检查', verCurrent: '已是最新版本。',
    verUnknown: '尚未检查。', verUnsupported: '此版本类型不能通过应用更新,这里也没有新版本检查。',
    verAvailable: v => `有可用的新版本 ${v}`, verCheck: '立即检查', verChecking: '正在检查 …', verInstall: '安装更新',
    verInstallConfirm: v => `下载版本 ${v} 并在电视上开始安装?电视会要求你确认。`,
    verRelease: '发布页面', verInstallOff: '已关闭通过应用安装。请在电视设置中开启:应用 → 通过应用安装更新。',
    verAutoOff: '每日检查已关闭(设置 → 关于 → 检查更新);“立即检查”仍可使用。',
    verDownloading: p => `正在下载 … ${p} %`, verVerifying: '已校验。请在电视上按确定键确认安装。',
    verErrors: { disabled: '已关闭通过应用安装。', none: '没有已知的新版本。请先检查。', need_permission: '请在电视上允许 ShrugTV“安装未知应用”,然后重试。',
      permission_unavailable: '这台电视没有“安装未知应用”的界面。', no_file: '该版本没有可安装的文件。', bad_checksum: '校验值不匹配,未安装任何内容。',
      download_failed: '下载失败。' },
    verNotes: '新增内容',
    favAdd: '标为收藏', favRemove: '取消收藏', favFailed: '无法保存星标。',
    listsLabel: '换台列表', listFollowed: '已关注', listSameGame: '相同分类', listSameGameLanguage: '相同分类,我的语言',
    listHistory: '最近观看', listTopStreams: '热门直播', listTopLanguage: '我的语言中的热门',
    listEmpty: '此列表目前没有内容。', listNeedsPlaying: '此列表需要有正在播放的直播:它显示当前播放频道的分类。',
    homeTitle: '添加到主屏幕', homeHint: '将此页面作为应用图标打开:',
    homeIos: 'iPhone:Safari → 分享 → 添加到主屏幕。', homeAndroid: 'Android:Chrome 菜单 ⋮ → 添加到主屏幕。'
  },
  pl: {
    pairText: 'Zeskanuj kod QR na telewizorze lub wpisz wyświetlony tam PIN.', pinLabel: 'PIN', connect: 'Połącz',
    pinWrong: 'PIN jest błędny lub wygasł. Pokaż nowy kod na telewizorze.', expired: 'Ten kod wygasł. Pokaż nowy na telewizorze.',
    offline: 'Telewizor jest niedostępny. Czy ShrugTV jest otwarte?', tabRemote: 'Pilot', tabChat: 'Czat', tabFind: 'Szukaj', tabPlayer: 'Odtwarzacz',
    up: 'Góra', down: 'Dół', left: 'Lewo', right: 'Prawo', back: 'Wstecz', more: 'Więcej', playPause: 'Odtwarzaj/Pauza',
    prev: '◀ Kanał', live: 'Na żywo', next: 'Kanał ▶', volume: 'Głośność transmisji', message: 'Wiadomość', send: 'Wyślij',
    chatOff: 'Pisanie na czacie z telefonu jest wyłączone w ustawieniach telewizora.', search: 'Szukaj', liveNow: 'Teraz na żywo',
    results: 'Wyniki', quality: 'Jakość', chatLayout: 'Układ czatu', layoutSide: 'Obok wideo',
    layoutOverlay: 'Nad wideo', layoutTransparent: 'Przezroczysty', chatOn: 'Czat wł.', chatOffBtn: 'Czat wył.',
    stats: 'Statystyki', close: 'Zamknij odtwarzacz', openTwitch: 'Otwórz na Twitchu', nothing: 'Nic nie jest odtwarzane',
    viewers: n => `${n.toLocaleString()} widzów`, since: m => `na żywo od ${Math.floor(m / 60)}:${String(m % 60).padStart(2, '0')} h`,
    behind: s => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')} za transmisją`, removed: 'wiadomość usunięta', offlineCh: 'offline',
    tabSettings: 'Ustawienia', tabRemoteShort: 'Pilot', tabChatShort: 'Czat', tabFindShort: 'Szukaj', tabPlayerShort: 'Odtw.', tabSettingsShort: 'Ustaw.',
    searchChannels: 'Nazwa kanału', tabLog: 'Dziennik',
    logOff: 'Dziennik w przeglądarce jest wyłączony na telewizorze (Ustawienia → Telefon i przeglądarka → Dziennik w przeglądarce).',
    logAbout: "Własny dziennik ShrugTV na tym telewizorze, na żywo. Tokeny i hasła są zawsze usuwane. Kliknij linię, aby zobaczyć szczegóły.",
    logSearch: 'Przeszukaj dziennik', logSource: 'Źródło', logAllSources: 'Wszystkie źródła', logLevel: 'Poziom',
    logAll: 'Wszystkie', logErrors: 'Błędy', logWarnings: 'Ostrzeżenia', logInfo: 'Info', logDebug: 'Debug',
    logPause: 'Pauza', logResume: 'Wznów', logCopy: 'Kopiuj', logExport: 'Zapisz .txt', logPrivate: 'Ukryj dane prywatne',
    logLatest: 'Przejdź do najnowszych ↓', logCopied: n => `Skopiowano linii: ${n}`, logSaved: n => `Zapisano linii: ${n}`,
    logShown: (n, total) => `${n} z ${total} linii`, logEmpty: 'Żadna linia nie pasuje.', logPaused: 'wstrzymano',
    logTime: 'Czas', logTag: 'Źródło', logThread: 'Proces/wątek', logMessage: 'Wiadomość', settingsOff: 'Zmiana ustawień z przeglądarki jest wyłączona na telewizorze (Ustawienia → Telefon i przeglądarka).',
    settingsSearch: 'Szukaj w ustawieniach', export: 'Zapisz do pliku', import: 'Wczytaj z pliku', saved: 'Zapisano', notSaved: 'Nie zapisano: niedozwolona wartość',
    imported: (n, m) => `Wczytano ustawień: ${n}` + (m ? `, pominięto: ${m}` : ''), importBad: 'To nie jest plik ustawień ShrugTV.',
    moveUp: 'W górę', run: 'Uruchom', done: 'Gotowe', tvOnly: 'Tylko na telewizorze', noScreen: 'Najpierw otwórz ShrugTV na telewizorze.', noResults: 'Nie znaleziono ustawień.',
    secretSet: 'zapisano – wpisz, aby zastąpić', secretEmpty: 'nie ustawiono', on: 'Wł.', off: 'Wył.', status: 'Status',
    pinned: 'Przypięte', pin: 'Przypnij do paska bocznego', unpin: 'Odepnij', noPins: 'Przypnij ustawienia przez ☆, aby znaleźć je tutaj.',
    exported: n => `Zapisano ustawień do pliku: ${n} (bez MQTT i aplikacji towarzyszącej, bo należą do tego telewizora).`,
    searchHint: '/ lub Ctrl+K: szukaj', theme: 'Jasny lub ciemny', guide: 'Przewodnik', guideShow: 'Pokaż przewodnik',
    connection: 'Połączenie', mqttBroker: 'Broker MQTT', noBroker: 'brak adresu brokera', entities: n => `encji: ${n}`,
    lastPublish: s => `wysłano ${ago(s, 'pl')}`, connectedFor: s => `połączono od ${ago(s, 'pl', true)}`, deviceId: 'ID urządzenia',
    stateTopic: 'Stan', commandTopic: 'Polecenia', devicesTitle: 'Połączone urządzenia', thisDevice: 'to urządzenie', remove: 'Usuń', justNow: 'przed chwilą',
    fromSkin: 'Z motywu', designTitle: 'Własne motywy', designSave: 'Zapisz bieżący wygląd jako motyw', designName: 'Nazwa',
    designExport: 'Eksportuj (.json)', designImport: 'Importuj', designDelete: n => `Usunąć motyw ${n}?`, designFull: 'Najwyżej 10 własnych motywów.',
    designBad: 'To nie jest plik wyglądu ShrugTV.', designNone: 'Brak własnych motywów.', designSaved: n => `Zapisano jako ${n}.`, designImported: n => `Zaimportowano jako ${n}.`,
    deviceSeen: (seen, paired) => `ostatnio użyte ${seen} · sparowane ${paired}`, removeDevice: n => `Usunąć ${n}? Będzie potrzebować kodu QR lub PIN-u ponownie.`,
    noDevices: 'Nie zapamiętano jeszcze żadnych urządzeń.', rememberOff: '„Zapamiętuj połączone urządzenia” jest wyłączone: urządzenia potrzebują kodu po każdym uruchomieniu aplikacji.', nameTaken: 'nazwa używana przez inny telewizor, dodano kod', noTls: 'bez TLS', reachable: 'dostępny',
    mqttStates: { connected: 'połączono', connecting: 'łączenie …', off: 'wyłączone', 'no broker address': 'brak adresu brokera' },
    remoteMode: 'Tryb pilota', modePad: 'Panel dotykowy', modeKeys: 'Przyciski', padLabel: 'Panel dotykowy: przesuń palcem dla strzałek, dotknij dla OK',
    padHint: 'Przesunięcie = strzałki\nDotknięcie = OK · Dwa palce = Wstecz', replay: '⟲ Powtórka', audioOnly: 'Tylko dźwięk',
    recent: 'Ostatnio oglądane', replyTo: n => `Odpowiedz do ${n}`, replyCancel: 'Anuluj odpowiedź', emotes: 'Emotki',
    noEmotes: 'Na tym czacie nie widziano jeszcze żadnych emotek. Pojawią się tutaj, gdy ktoś jakiejś użyje.', quickAdd: 'Dodaj szybką frazę',
    quickNew: 'Nowa szybka fraza (maks. 60 znaków)', quickRemove: t => `Usunąć "${t}" z szybkich fraz?`,
    chatHint: 'Dotknij wiadomości, aby odpowiedzieć przez @nazwa. Przytrzymaj szybką frazę, aby ją usunąć.',
    tabUpdate: 'Wersja', tabUpdateShort: 'Info', verInstalled: 'Zainstalowana', verEdition: 'Edycja', verEditionPublic: 'Publiczna', verEditionPrivate: 'Prywatna',
    verDevice: 'Telewizor', verAndroid: 'Android', verLastCheck: 'Ostatnie sprawdzenie', verNever: 'jeszcze nie', verCurrent: 'Masz najnowszą wersję.',
    verUnknown: 'Jeszcze nie sprawdzono.', verUnsupported: 'Ta edycja nie jest aktualizowana z aplikacji, a sprawdzania nowych wersji tu nie ma.',
    verAvailable: v => `Dostępna jest wersja ${v}`, verCheck: 'Sprawdź teraz', verChecking: 'Sprawdzanie …', verInstall: 'Zainstaluj aktualizację',
    verInstallConfirm: v => `Pobrać wersję ${v} i rozpocząć instalację na telewizorze? Telewizor poprosi o potwierdzenie.`,
    verRelease: 'Strona wydania', verInstallOff: 'Instalowanie z aplikacji jest wyłączone. Włącz je w ustawieniach telewizora: Aplikacja → Instaluj aktualizacje w aplikacji.',
    verAutoOff: 'Codzienne sprawdzanie jest wyłączone (Ustawienia → O aplikacji → Sprawdzaj aktualizacje); "Sprawdź teraz" i tak działa.',
    verDownloading: p => `Pobieranie … ${p} %`, verVerifying: 'Sprawdzono. Potwierdź instalację na telewizorze przyciskiem OK.',
    verErrors: { disabled: 'Instalowanie z aplikacji jest wyłączone.', none: 'Brak znanej nowszej wersji. Najpierw sprawdź.', need_permission: 'Zezwól ShrugTV na "Instalowanie nieznanych aplikacji" na telewizorze i spróbuj ponownie.',
      permission_unavailable: 'Ten telewizor nie ma ekranu "Instalowanie nieznanych aplikacji".', no_file: 'Wydanie nie ma pliku do zainstalowania.', bad_checksum: 'Suma kontrolna się nie zgadza, nic nie zainstalowano.',
      download_failed: 'Pobieranie nie powiodło się.' },
    verNotes: 'Co nowego',
    favAdd: 'Oznacz jako ulubiony', favRemove: 'Usuń z ulubionych', favFailed: 'Nie udało się zapisać gwiazdki.',
    listsLabel: 'Listy przełącznika kanałów', listFollowed: 'Obserwowane', listSameGame: 'Ta sama kategoria', listSameGameLanguage: 'Ta sama kategoria, mój język',
    listHistory: 'Ostatnio oglądane', listTopStreams: 'Najpopularniejsze transmisje', listTopLanguage: 'Najpopularniejsze w moim języku',
    listEmpty: 'Na tej liście na razie nic nie ma.', listNeedsPlaying: 'Ta lista wymaga trwającej transmisji: pokazuje kategorię odtwarzanego kanału.',
    homeTitle: 'Na ekranie głównym', homeHint: 'Otwórz tę stronę jako ikonę aplikacji:',
    homeIos: 'iPhone: Safari → Udostępnij → Dodaj do ekranu początkowego.', homeAndroid: 'Android: menu Chrome ⋮ → Dodaj do ekranu głównego.'
  },
};
// "55 s ago" / "vor 55 s"; plain duration with bare = true
const AGO = {
  en: d => `${d} ago`, de: d => `vor ${d}`, es: d => `hace ${d}`, fr: d => `il y a ${d}`, pt: d => `há ${d}`, it: d => `${d} fa`,
  pl: d => `${d} temu`, tr: d => `${d} önce`, ru: d => `${d} назад`, ja: d => `${d}前`, ko: d => `${d} 전`, zh: d => `${d}前`
};
function ago(s, lang, bare) {
  const d = s < 60 ? `${s} s` : s < 3600 ? `${Math.floor(s / 60)} min` : `${Math.floor(s / 3600)} h`;
  if (bare) return d;
  return (AGO[lang] || AGO.en)(d);
}
// The browser's language if the page has it, English otherwise
const LANG = ['en', 'de', 'es', 'fr', 'pt', 'it', 'pl', 'tr', 'ru', 'ja', 'ko', 'zh']
  .find(c => (navigator.language || 'en').toLowerCase().startsWith(c) && T[c]) || 'en';
// Texts a language does not have yet show in English
const L = new Proxy(T[LANG], { get: (t, k) => (k in t ? t[k] : T.en[k]) });
document.documentElement.lang = LANG;

const $ = id => document.getElementById(id);
let ws = null;
let state = {};
let chatOn = false;
let reconnectDelay = 1000;

function applyTexts() {
  for (const el of document.querySelectorAll('[data-i18n]')) el.textContent = L[el.dataset.i18n];
  for (const el of document.querySelectorAll('[data-i18n-label]')) el.setAttribute('aria-label', L[el.dataset.i18nLabel]);
  for (const el of document.querySelectorAll('[data-i18n-placeholder]')) el.placeholder = L[el.dataset.i18nPlaceholder];
  for (const el of document.querySelectorAll('[data-i18n-title]')) el.title = L[el.dataset.i18nTitle];
}

async function api(path, options) {
  const r = await fetch(path, Object.assign({ credentials: 'same-origin' }, options || {}));
  if (r.status === 401) throw new Error('unpaired');
  return r.json();
}

function showPair(message) {
  $('app').hidden = true;
  $('pair').hidden = false;
  $('pair-error').textContent = message || '';
}

async function start() {
  applyTexts();
  if (new URLSearchParams(location.search).has('expired')) history.replaceState(null, '', '/');
  try {
    const s = await api('/api/state');
    $('pair').hidden = true;
    $('app').hidden = false;
    render(s);
    connect();
    routeFromHash();
    updateBadge();
    loadSkin();
  } catch (e) {
    showPair(location.search.includes('expired') ? L.expired : '');
  }
}

$('pin-form').addEventListener('submit', async ev => {
  ev.preventDefault();
  const r = await fetch('/api/pin', { method: 'POST', body: JSON.stringify({ pin: $('pin').value }), credentials: 'same-origin' });
  if (r.ok) start(); else showPair(L.pinWrong);
});

function connect() {
  ws = new WebSocket(`ws://${location.host}/ws`);
  ws.onopen = () => {
    $('offline').hidden = true;
    reconnectDelay = 1000;
    if (chatOn) send({ t: 'chat', on: true });
  };
  ws.onmessage = ev => {
    const msg = JSON.parse(ev.data);
    if (msg.t === 'state') render(msg.d);
    else if (msg.t === 'chat') addChat(msg.d.lines);
    else if (msg.t === 'settings') { settingsChanged(msg.d.key); if (/^pref_(theme_|skin$|custom_skins$|companion_skin$)/.test(msg.d.key || '')) loadSkin(); }
  };
  ws.onclose = ev => {
    $('offline').hidden = false;
    if (ev.code === 1008) { showPair(''); return; } // session no longer valid
    setTimeout(connect, reconnectDelay);
    reconnectDelay = Math.min(reconnectDelay * 2, 15000);
  };
}

function send(obj) {
  if (ws && ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(obj));
}
const key = k => send({ t: 'key', k });
const cmd = (name, payload) => send({ t: 'cmd', name, payload: payload == null ? '' : String(payload) });

function render(s) {
  state = s;
  const active = s.kind && s.kind !== 'none';
  $('now-avatar').src = s.avatar_url || '';
  $('now-avatar').hidden = !s.avatar_url;
  $('now-channel').textContent = active ? (s.channel || s.login || '') : L.nothing;
  const live = $('now-live');
  live.textContent = s.kind === 'live' ? 'LIVE' : '';
  live.classList.toggle('behind', s.behind_live_s > 30);
  const meta = [];
  if (s.kind === 'live') {
    if (s.viewers) meta.push(L.viewers(s.viewers));
    if (s.behind_live_s > 30) meta.push(L.behind(s.behind_live_s));
    else if (s.uptime_min != null) meta.push(L.since(s.uptime_min));
  }
  if (s.language) meta.push(s.language);
  $('now-meta').textContent = meta.join(' · ');
  $('now-title').textContent = [s.category, s.title].filter(Boolean).join(' · ');
  if (document.activeElement !== $('volume')) $('volume').value = s.volume == null ? 100 : s.volume;
  const q = $('quality');
  const opts = (s.qualities || []).join('|');
  if (q.dataset.opts !== opts) {
    q.replaceChildren(...(s.qualities || []).map(name => new Option(name, name)));
    q.dataset.opts = opts;
  }
  q.value = s.quality || 'Auto';
  $('layout').value = s.chat_layout || 'side_by_side';
  $('chat-form').hidden = !s.chat_allowed;
  $('chat-off').hidden = !!s.chat_allowed;
  $('btn-replay').hidden = s.kind !== 'live';
  const ao = $('btn-audio');
  ao.hidden = !active;
  ao.setAttribute('aria-pressed', String(!!s.audio_only));
  if (active && s.login) rememberChannel(s);
  paintNowStar();
  $('open-twitch').href = s.login ? `https://www.twitch.tv/${encodeURIComponent(s.login)}` : '#';
}

// ---------------------------------------------------------------- chat
function addChat(lines) {
  const box = $('chat');
  const atBottom = box.scrollHeight - box.scrollTop - box.clientHeight < 40;
  for (const l of lines) {
    if (l.upd) {
      const old = box.querySelector(`[data-id="${l.id}"]`);
      if (old) markDeleted(old);
      continue;
    }
    const div = document.createElement('div');
    div.className = 'msg' + (l.no ? ' notice' : '') + (l.hl ? ' hl' : '');
    div.dataset.id = l.id;
    if (l.n && !l.no) div.dataset.name = l.n;
    for (const e of l.e || []) noteEmote((l.x || '').slice(e[0], e[1] + 1), e[2]);
    if (l.r) {
      const r = document.createElement('span');
      r.className = 'r';
      r.textContent = '↪ ' + l.r;
      div.append(r);
    }
    if (l.n && !l.no) {
      for (const url of l.b || []) {
        if (!/^https:\/\//.test(url)) continue;
        const b = document.createElement('img');
        b.className = 'b';
        b.src = url;
        b.alt = '';
        div.append(b);
      }
      const n = document.createElement('span');
      n.className = 'n';
      n.style.color = /^#[0-9A-F]{6}$/i.test(l.c) ? l.c : '';
      n.textContent = l.n;
      div.append(n, document.createTextNode(': '));
    }
    if (l.d) { div.append(document.createTextNode(L.removed)); div.classList.add('del'); }
    else div.append(...withEmotes(l.x || '', l.e || []));
    box.append(div);
  }
  while (box.children.length > 200) box.firstChild.remove();
  if (atBottom) box.scrollTop = box.scrollHeight;
}

function markDeleted(el) {
  const name = el.querySelector('.n');
  el.replaceChildren(...(name ? [name, document.createTextNode(': ')] : []), document.createTextNode(L.removed));
  el.classList.add('del');
}

// Text with emote images at their positions [start, end, url] (end inclusive, UTF-16)
function withEmotes(text, emotes) {
  const parts = [];
  const sorted = emotes.slice().sort((a, b) => a[0] - b[0]);
  let pos = 0;
  for (const [s, e, url] of sorted) {
    if (s < pos || !/^https:\/\//.test(url)) continue;
    if (s > pos) parts.push(document.createTextNode(text.slice(pos, s)));
    const img = document.createElement('img');
    img.src = url;
    img.alt = text.slice(s, e + 1);
    img.loading = 'lazy';
    parts.push(img);
    pos = e + 1;
  }
  if (pos < text.length) parts.push(document.createTextNode(text.slice(pos)));
  return parts;
}

$('chat-form').addEventListener('submit', ev => {
  ev.preventDefault();
  let text = $('chat-input').value.trim();
  if (text && replyTo && !/^@/.test(text)) text = `@${replyTo} ${text}`;
  if (text) cmd('chat_message', text.slice(0, 500));
  $('chat-input').value = '';
  setReply(null);
  showEmotes(false);
});

// ---------------------------------------------------------------- find
function renderCards(items, title) {
  $('find-title').textContent = title;
  $('cards').replaceChildren(...items.map(it => {
    const li = document.createElement('li');
    const b = document.createElement('button');
    const img = document.createElement('img');
    img.alt = '';
    if (it.avatar || it.image) img.src = it.avatar || it.image;
    const t = document.createElement('div');
    t.className = 't';
    const name = document.createElement('div');
    name.textContent = (it.name || it.login) + (it.live === false ? ` · ${L.offlineCh}` : '');
    name.style.fontWeight = '700';
    const meta = document.createElement('div');
    meta.className = 'muted';
    meta.textContent = [it.category, it.viewers ? L.viewers(it.viewers) : ''].filter(Boolean).join(' · ');
    t.append(name, meta);
    if (it.title) {
      const title = document.createElement('div');
      title.className = 'muted title';
      title.textContent = it.title;
      t.append(title);
    }
    b.append(img, t);
    b.addEventListener('click', () => { if (it.login) cmd('watch', it.login); });
    li.append(b);
    if (it.id && /^[0-9]{1,12}$/.test(it.id)) {
      li.classList.add('has-star');
      li.append(starButton(it.id, !!it.favorite));
    }
    return li;
  }));
}

// The lists of the TV's channel switcher: the same channels, one tap starts one on the TV
const LIST_KEYS = { followed: 'listFollowed', same_game: 'listSameGame', same_game_language: 'listSameGameLanguage',
  history: 'listHistory', top_streams: 'listTopStreams', top_language: 'listTopLanguage' };
let currentList = 'followed';
let listEnabled = Object.keys(LIST_KEYS);

function renderListChips() {
  $('lists').replaceChildren(...listEnabled.filter(n => LIST_KEYS[n]).map(n => {
    const b = document.createElement('button');
    b.type = 'button';
    b.textContent = L[LIST_KEYS[n]];
    b.setAttribute('aria-pressed', String(n === currentList && !$('search').value.trim()));
    b.addEventListener('click', () => { $('search').value = ''; currentList = n; loadLive(); });
    return b;
  }));
}

async function loadLive() {
  renderListChips();
  try {
    const r = await api('/api/lists?name=' + encodeURIComponent(currentList));
    if (Array.isArray(r.enabled)) { listEnabled = r.enabled; if (!listEnabled.includes(currentList)) currentList = listEnabled[0] || 'followed'; renderListChips(); }
    renderCards(r.items || [], L[LIST_KEYS[currentList]] || L.liveNow);
    let empty = $('cards-empty');
    if (!empty) { empty = document.createElement('p'); empty.id = 'cards-empty'; empty.className = 'muted'; $('cards').after(empty); }
    const needsPlaying = (currentList === 'same_game' || currentList === 'same_game_language') && !r.playing;
    empty.textContent = (r.items || []).length ? '' : (needsPlaying ? L.listNeedsPlaying : L.listEmpty);
  } catch (e) { /* not paired or offline */ }
}

$('search-form').addEventListener('submit', async ev => {
  ev.preventDefault();
  const q = $('search').value.trim();
  if (!q) { loadLive(); return; }
  try {
    renderCards((await api('/api/search?q=' + encodeURIComponent(q))).items, L.results);
    renderListChips();
    const e = $('cards-empty');
    if (e) e.textContent = '';
  } catch (e) { /* ignore */ }
});

// ---------------------------------------------------------------- tabs, deep links and controls
// Every tab and settings group has its own address: #remote, #chat, #find, #player, #settings,
// #settings/<group key>, #settings/pinned (bookmarkable, like Glasshouse's ?tab=…)
const TABS = ['remote', 'chat', 'find', 'player', 'update', 'settings', 'log'];
let currentTab = 'remote';

function showTab(name, updateHash) {
  if (!TABS.includes(name)) name = 'remote';
  currentTab = name;
  for (const t of document.querySelectorAll('[role=tab]')) t.setAttribute('aria-selected', String(t.dataset.tab === name));
  for (const s of document.querySelectorAll('.tab')) s.hidden = s.id !== 'tab-' + name;
  $('set-groups').hidden = name !== 'settings';
  chatOn = name === 'chat';
  send({ t: 'chat', on: chatOn });
  if (name === 'find') loadLive();
  if (name === 'settings') loadSettings();
  if (name === 'log') logStart(); else logStop();
  if (name === 'update') updateStart(); else updateStop();
  if (updateHash !== false) setHash();
}

function setHash() {
  let h = '#' + currentTab;
  if (currentTab === 'settings' && setTree) h += '/' + (setGroup === PINNED ? 'pinned' : (setTree.pages[setGroup] || {}).key || '');
  if (location.hash !== h) history.replaceState(null, '', location.pathname + location.search + h);
}

let wantedGroup = null; // group key from the address, applied once the tree is loaded
function routeFromHash() {
  const [tab, group] = location.hash.replace(/^#/, '').split('/');
  wantedGroup = group || null;
  if (group) $('set-search').value = '';
  if (tab === currentTab && tab === 'settings' && setTree) { applyWantedGroup(); renderGroups(); renderSettings(); return; }
  showTab(tab || 'remote', false);
}
window.addEventListener('hashchange', routeFromHash);

for (const tab of document.querySelectorAll('[role=tab]')) tab.addEventListener('click', () => showTab(tab.dataset.tab));
for (const b of document.querySelectorAll('[data-key]')) b.addEventListener('click', () => key(b.dataset.key));
for (const b of document.querySelectorAll('[data-cmd]')) b.addEventListener('click', () => cmd(b.dataset.cmd, b.dataset.payload));
$('volume').addEventListener('change', ev => cmd('volume', ev.target.value));
$('quality').addEventListener('change', ev => cmd('quality', ev.target.value));
$('layout').addEventListener('change', ev => cmd('chat_layout', ev.target.value));

// ---------------------------------------------------------------- theme and keyboard
function applyTheme(t) {
  document.documentElement.dataset.theme = t;
  document.querySelector('meta[name=theme-color]').content = t === 'light' ? '#F7F7F8' : '#0B0B0E';
  $('theme').textContent = t === 'light' ? '☾' : '☀'; // shows what a click switches to
}
$('theme').addEventListener('click', () => {
  const t = document.documentElement.dataset.theme === 'light' ? 'dark' : 'light';
  applyTheme(t);
  store('shrugtv.theme', t);
});
applyTheme(document.documentElement.dataset.theme === 'light' ? 'light' : 'dark');
$('set-search').value = ''; // browsers restore form fields on reload

// "/" or Ctrl/Cmd+K: search the settings from anywhere (computer keyboard)
document.addEventListener('keydown', ev => {
  const typing = ev.target && (ev.target.tagName === 'INPUT' || ev.target.tagName === 'SELECT' || ev.target.tagName === 'TEXTAREA');
  if ((ev.key === 'k' && (ev.ctrlKey || ev.metaKey)) || (ev.key === '/' && !typing)) {
    if ($('app').hidden) return;
    ev.preventDefault();
    if (currentTab !== 'settings') showTab('settings');
    $('set-search').focus();
    $('set-search').select();
  } else if (ev.key === 'Escape' && ev.target === $('set-search') && $('set-search').value) {
    $('set-search').value = '';
    renderGroups();
    renderSettings();
  }
});

// Small per-browser memory (theme, pinned settings, opened sections)
function load(name, fallback) {
  try { const v = JSON.parse(localStorage.getItem(name)); return v == null ? fallback : v; } catch (e) { return fallback; }
}
function store(name, value) {
  try { localStorage.setItem(name, typeof value === 'string' ? value : JSON.stringify(value)); } catch (e) { /* private mode */ }
}

// ---------------------------------------------------------------- settings (all app settings, docs/companion_plan.md 8)
const PINNED = -1;
let setTree = null;
let setGroup = 0;
let setValues = {};
let setTimer = null;
let pins = load('shrugtv.pins', []);
let openSections = load('shrugtv.open', []);
const ownKeys = new Map(); // key -> time of our own change (skip the echo from the TV)

async function loadSettings() {
  try {
    const r = await fetch('/api/settings?lang=' + LANG, { credentials: 'same-origin' });
    if (r.status === 401) { showPair(''); return; }
    const off = r.status === 403;
    $('set-off').hidden = !off;
    $('set-ui').hidden = off;
    if (off) { $('set-groups').replaceChildren(); return; }
    setTree = await r.json();
    setValues = Object.assign({}, setTree.standards);
    collectValues(setTree.pages);
    applyWantedGroup();
    const focusKey = document.activeElement && document.activeElement.dataset ? document.activeElement.dataset.key : null;
    renderGroups();
    renderSettings();
    setHash();
    if (focusKey) {
      const el = document.querySelector(`#set-content [data-key="${CSS.escape(focusKey)}"]`);
      if (el) el.focus();
    }
  } catch (e) { /* offline: the banner says so */ }
}

function applyWantedGroup() {
  if (!wantedGroup || !setTree) return;
  if (wantedGroup === 'pinned') setGroup = PINNED;
  else {
    const i = setTree.pages.findIndex(p => p.key === wantedGroup);
    if (i >= 0) setGroup = i;
  }
  wantedGroup = null;
}

function collectValues(list) {
  for (const e of list) {
    if (e.children) collectValues(e.children);
    else if (e.key && e.value !== undefined) setValues[e.key] = e.value;
  }
}

/** Every setting with the titles of the pages above it. */
function allEntries() {
  const out = [];
  const walk = (list, path) => {
    for (const e of list) {
      if (e.children) walk(e.children, e.type === 'page' ? path.concat(e.title) : path);
      else out.push([e, path]);
    }
  };
  walk(setTree.pages, []);
  return out;
}

function settingsChanged(key) {
  const own = ownKeys.get(key);
  if (own && Date.now() - own < 3000) return;
  if (currentTab !== 'settings') return;
  // Do not pull the field from under the user's fingers while typing
  const a = document.activeElement;
  if (a && a.tagName === 'INPUT' && a.type !== 'checkbox' && $('tab-settings').contains(a)) return;
  clearTimeout(setTimer);
  setTimer = setTimeout(loadSettings, 400);
}

function groupButton(text, current, onClick, cls) {
  const b = document.createElement('button');
  b.type = 'button';
  b.textContent = text;
  if (cls) b.className = cls;
  b.setAttribute('aria-current', String(current));
  b.addEventListener('click', onClick);
  return b;
}

function selectGroup(i) {
  setGroup = i;
  $('set-search').value = '';
  renderGroups();
  renderSettings();
  setHash();
  $('set-content').scrollIntoView({ block: 'start', behavior: 'smooth' });
}

function renderGroups() {
  if (!setTree) return;
  const pages = setTree.pages;
  if (setGroup >= pages.length || (setGroup === PINNED && !pins.length)) setGroup = 0;
  const searching = !!searchText();
  const buttons = [];
  if (pins.length) buttons.push(groupButton('★ ' + L.pinned, setGroup === PINNED && !searching, () => selectGroup(PINNED), 'pin-group'));
  pages.forEach((p, i) => buttons.push(groupButton(p.title, i === setGroup && !searching, () => selectGroup(i))));
  $('set-groups').replaceChildren(...buttons);
}

const searchText = () => $('set-search').value.trim().toLowerCase();

function renderSettings() {
  const box = $('set-content');
  const q = searchText();
  if (q) {
    const hits = allEntries().filter(([e]) => e.type !== 'tip' && [e.title, e.text, e.key].some(t => t && t.toLowerCase().includes(q)));
    box.replaceChildren(hits.length ? flatGroup(hits) : para(L.noResults, 'muted'));
  } else if (setGroup === PINNED) {
    const byKey = new Map(allEntries().filter(([e]) => e.key).map(x => [x[0].key, x]));
    const list = pins.map(k => byKey.get(k)).filter(Boolean);
    const h = document.createElement('h2');
    h.textContent = '★ ' + L.pinned;
    box.replaceChildren(h, list.length ? flatGroup(list) : para(L.noPins, 'muted'));
  } else {
    const page = setTree.pages[setGroup];
    const h = document.createElement('h2');
    h.textContent = page.title;
    box.replaceChildren(h, ...(page.text ? [para(page.text, 'muted')] : []), ...renderList(page.children, 0));
  }
  updateActive();
  if ($('mqtt-card')) refreshMqtt();
}

// ---------------------------------------------------------------- MQTT connection card
const MQTT_GROUPS = { pref_mqtt_entities_info: 'info', pref_mqtt_entities_avatar: 'avatar', pref_mqtt_entities_preview: 'preview', pref_mqtt_entities_events: 'events', pref_mqtt_entities_followed: 'followed', pref_mqtt_commands: 'controls' };
let mqttTimer = null;

function mqttCard() {
  const card = document.createElement('div');
  card.id = 'mqtt-card';
  card.className = 'conn';
  const h = document.createElement('div');
  h.className = 'conn-title';
  h.textContent = L.connection;
  card.append(h, connRow('tv', 'ShrugTV', L.reachable + (setTree.version ? ` · ${setTree.version}` : ''), 'ok'), connRow('mqtt', L.mqttBroker, '…', 'off'));
  const facts = document.createElement('dl');
  facts.className = 'conn-facts';
  card.append(facts);
  return card;
}

function connRow(id, name, sub, dot) {
  const row = document.createElement('div');
  row.className = 'conn-row';
  row.id = 'conn-' + id;
  const icon = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  icon.setAttribute('viewBox', '0 0 24 24');
  icon.setAttribute('aria-hidden', 'true');
  const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
  path.setAttribute('d', id === 'tv' ? 'M3 5h18v12H3zM8 21h8M12 17v4' : 'M6 12a2 2 0 1 0 0 .01M18 6a2 2 0 1 0 0 .01M18 18a2 2 0 1 0 0 .01M8 11l8-4M8 13l8 4');
  icon.append(path);
  const text = document.createElement('div');
  text.className = 'conn-text';
  const n = document.createElement('strong');
  n.textContent = name;
  const s = document.createElement('span');
  s.className = 'muted conn-sub';
  s.textContent = sub;
  text.append(n, s);
  const d = document.createElement('span');
  d.className = 'dot ' + dot;
  row.append(icon, text, d);
  return row;
}

async function refreshMqtt() {
  clearTimeout(mqttTimer);
  const card = $('mqtt-card');
  if (!card || currentTab !== 'settings') return;
  try {
    const r = await fetch('/api/mqtt', { credentials: 'same-origin' });
    if (!r.ok) return;
    const m = await r.json();
    const row = $('conn-mqtt');
    const status = L.mqttStates[m.status] || m.status;
    row.querySelector('strong').textContent = m.broker ? m.broker.replace(/^tcp:\/\//, '') : L.mqttBroker;
    const sub = [m.connected_s != null ? L.connectedFor(m.connected_s) : status];
    if (m.last_publish_s != null) sub.push(L.lastPublish(m.last_publish_s));
    if (m.broker && !m.tls) sub.push(L.noTls);
    row.querySelector('.conn-sub').textContent = sub.join(' · ');
    row.querySelector('.dot').className = 'dot ' + (m.connected ? 'ok' : m.status === 'off' ? 'off' : m.status === 'connecting' ? 'wait' : 'bad');
    const facts = card.querySelector('.conn-facts');
    facts.replaceChildren(...[[L.deviceId, m.device_id + (m.name_taken ? ` (${L.nameTaken})` : '')], [L.stateTopic, m.state_topic], [L.commandTopic, m.command_topic]].flatMap(([k, v]) => {
      const dt = document.createElement('dt');
      dt.textContent = k;
      const dd = document.createElement('dd');
      dd.textContent = v || '–';
      return [dt, dd];
    }));
    for (const [key, group] of Object.entries(MQTT_GROUPS)) {
      const title = document.querySelector(`[data-row="${key}"] .set-title`);
      if (!title || m.counts[group] == null) continue;
      let c = title.parentElement.querySelector('.count');
      if (!c) { c = document.createElement('span'); c.className = 'count muted'; title.after(c); }
      c.textContent = L.entities(m.counts[group]);
    }
  } catch (e) { /* offline */ }
  mqttTimer = setTimeout(refreshMqtt, 5000);
}

// ---------------------------------------------------------------- remembered devices
function devicesCard() {
  const card = document.createElement('div');
  card.id = 'devices-card';
  card.className = 'conn';
  const h = document.createElement('div');
  h.className = 'conn-title';
  h.textContent = L.devicesTitle;
  card.append(h, para('…', 'muted'));
  setTimeout(loadDevices, 0);
  return card;
}

function relTime(t) {
  const s = Math.max(0, Math.round((Date.now() - t) / 1000));
  if (s < 90) return L.justNow;
  return ago(s, LANG);
}

async function loadDevices() {
  const card = $('devices-card');
  if (!card) return;
  try {
    const r = await fetch('/api/devices', { credentials: 'same-origin' });
    if (!r.ok) return;
    const d = await r.json();
    const rows = d.devices.map(dev => {
      const row = document.createElement('div');
      row.className = 'conn-row';
      const text = document.createElement('div');
      text.className = 'conn-text';
      const n = document.createElement('strong');
      n.textContent = dev.name + (dev.id === d.current ? ` (${L.thisDevice})` : '');
      const s = document.createElement('span');
      s.className = 'muted conn-sub';
      s.textContent = L.deviceSeen(relTime(dev.seen), new Date(dev.paired).toLocaleDateString(LANG));
      text.append(n, s);
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'small';
      b.textContent = L.remove;
      b.addEventListener('click', async () => {
        if (!confirm(L.removeDevice(dev.name))) return;
        try {
          await postJson('/api/devices/remove', { id: dev.id });
          // Removing this device ends its pairing: the page asks for the code again
          if (dev.id === d.current) { showPair(''); return; }
          loadDevices();
        } catch (e) { /* offline */ }
      });
      row.append(text, b);
      return row;
    });
    const head = card.querySelector('.conn-title');
    card.replaceChildren(head, ...(rows.length ? rows : [para(d.remember ? L.noDevices : L.rememberOff, 'muted')]));
  } catch (e) { /* offline */ }
}

/** Search results and pinned settings: one box, each row with its place in the settings. */
function flatGroup(items) {
  const group = document.createElement('section');
  group.className = 'set-group';
  group.append(...items.map(([e, path]) => {
    const row = renderEntry(e);
    row.prepend(para(path.join(' › '), 'crumb'));
    return row;
  }));
  return group;
}

function para(text, cls) {
  const p = document.createElement('p');
  p.className = cls || '';
  p.textContent = text;
  return p;
}

function renderList(list, depth) {
  const out = [];
  for (const e of list) {
    if (e.type === 'page') {
      // Sub-pages (clock, remote shortcuts, filters …) are folded away; the browser remembers which are open
      const d = document.createElement('details');
      d.className = 'set-group sub';
      if (e.dep) d.dataset.dep = e.dep;
      d.open = openSections.includes(e.key);
      if (e.key) d.dataset.section = e.key;
      const sum = document.createElement('summary');
      sum.textContent = e.title;
      d.append(sum);
      if (e.text) d.append(para(e.text, 'muted'));
      d.append(...renderList(e.children, depth + 1));
      d.addEventListener('toggle', () => {
        if (!e.key) return;
        openSections = openSections.filter(k => k !== e.key);
        if (d.open) openSections.push(e.key);
        store('shrugtv.open', openSections);
      });
      out.push(d);
    } else if (e.type === 'category' || e.type === 'page') {
      const sec = document.createElement('section');
      sec.className = 'set-group';
      if (e.dep) sec.dataset.dep = e.dep;
      const h = document.createElement(depth === 0 ? 'h3' : 'h4');
      h.className = 'set-heading';
      const ht = document.createElement('span');
      ht.textContent = e.title;
      h.append(ht);
      sec.append(h);
      if (e.text) sec.append(para(e.text, 'muted'));
      // The group's guide sits behind an (i) next to its title instead of taking room in the list
      const guide = e.children.find(c => c.type === 'tip');
      if (guide) {
        const box = para(guide.text || guide.title, 'set-tip');
        box.hidden = true;
        box.id = 'tip-' + Math.random().toString(36).slice(2);
        h.append(infoButton(box));
        sec.append(box);
      }
      // The connection card replaces the "Connection status" button in the browser
      const own = e.children.filter(c => c !== guide && !(e.panel === 'mqtt' && c.key === 'pref_mqtt_status') &&
        !(e.panel === 'devices' && c.key === 'pref_companion_devices'));
      const children = renderList(own, depth + 1);
      if (e.panel === 'mqtt') children.unshift(mqttCard()); // live card, like Glasshouse's MQTT page
      if (e.panel === 'devices') children.push(devicesCard());
      if (e.panel === 'design') children.push(designCard());
      sec.append(...children);
      out.push(sec);
    } else {
      out.push(renderEntry(e));
    }
  }
  return out;
}

function pinButton(e) {
  const b = document.createElement('button');
  b.type = 'button';
  b.className = 'pin';
  const on = pins.includes(e.key);
  b.textContent = on ? '★' : '☆';
  b.setAttribute('aria-pressed', String(on));
  b.setAttribute('aria-label', on ? L.unpin : L.pin);
  b.title = on ? L.unpin : L.pin;
  b.addEventListener('click', () => {
    pins = pins.includes(e.key) ? pins.filter(k => k !== e.key) : pins.concat(e.key);
    store('shrugtv.pins', pins);
    renderGroups();
    if (setGroup === PINNED || !pins.length) renderSettings();
    else {
      const now = pins.includes(e.key);
      b.textContent = now ? '★' : '☆';
      b.setAttribute('aria-pressed', String(now));
      b.setAttribute('aria-label', now ? L.unpin : L.pin);
      b.title = now ? L.unpin : L.pin;
    }
  });
  return b;
}

function renderEntry(e) {
  // The live preview is the TV's; the browser changes the TV directly
  if (e.type === 'preview') return document.createComment('preview');
  const row = document.createElement('div');
  if (e.type === 'tip') {
    // A guide outside a group (sub-pages, search): a small (i) line that opens the text
    row.className = 'set-tip-line';
    const box = para(e.text || e.title, 'set-tip');
    box.hidden = true;
    box.id = 'tip-' + Math.random().toString(36).slice(2);
    const label = document.createElement('span');
    label.className = 'muted';
    label.textContent = L.guide;
    row.append(infoButton(box), label, box);
    return row;
  }
  row.className = 'set-row';
  if (e.dep) row.dataset.dep = e.dep;
  if (e.key) row.dataset.row = e.key;
  const label = document.createElement('div');
  label.className = 'set-label';
  const id = 'set-' + (e.key || Math.random().toString(36).slice(2));
  const head = document.createElement('div');
  head.className = 'set-head';
  const title = document.createElement(e.type === 'action' ? 'span' : 'label');
  title.className = 'set-title';
  title.textContent = e.place ? `${e.place}. ${e.title}` : e.title;
  if (e.type !== 'action') title.htmlFor = id;
  head.append(title);
  if (e.key && e.type !== 'action') head.append(pinButton(e));
  label.append(head);
  const desc = toggleText(e);
  if (desc) label.append(para(desc, 'muted desc'));
  row.append(label);

  let control = null;
  if (e.type === 'toggle') {
    control = document.createElement('input');
    control.type = 'checkbox';
    control.className = 'switch';
    control.checked = !!setValues[e.key];
    control.addEventListener('change', () => save(e, control.checked, row, control));
  } else if (e.type === 'list') {
    control = document.createElement('select');
    control.replaceChildren(...e.options.map(([v, n]) => new Option(n, v)));
    control.value = e.value;
    control.addEventListener('change', () => save(e, control.value, row, control));
  } else if (e.type === 'number') {
    control = document.createElement('input');
    control.type = 'number';
    control.min = e.min;
    control.max = e.max;
    control.step = e.step;
    control.inputMode = 'numeric';
    control.value = e.value;
    control.addEventListener('change', () => {
      const n = Math.round(Number(control.value));
      if (control.value === '' || !Number.isFinite(n)) { revert(e, control); return; }
      const clamped = Math.min(e.max, Math.max(e.min, n));
      control.value = clamped;
      save(e, clamped, row, control);
    });
    control.addEventListener('keydown', ev => { if (ev.key === 'Enter') control.blur(); });
  } else if (e.type === 'text') {
    control = document.createElement('input');
    control.maxLength = 500;
    control.autocomplete = 'off';
    if (e.secret) {
      control.type = 'password';
      control.placeholder = e.isSet ? L.secretSet : L.secretEmpty;
    } else {
      control.value = e.value || '';
    }
    control.addEventListener('change', () => { if (!e.secret || control.value) save(e, control.value, row, control); });
    control.addEventListener('keydown', ev => { if (ev.key === 'Enter') control.blur(); });
  } else if (e.type === 'color') {
    // A colour of the design: colour field plus "from the skin"
    control = document.createElement('span');
    control.className = 'color-pick';
    const field = document.createElement('input');
    field.type = 'color';
    field.value = (e.value && e.value !== 'skin' ? e.value : e.effective || '#9147ff').toLowerCase();
    const reset = document.createElement('button');
    reset.type = 'button';
    reset.className = 'small';
    reset.textContent = L.fromSkin;
    reset.hidden = !e.value || e.value === 'skin';
    field.addEventListener('change', async () => {
      await save(e, field.value.toUpperCase(), row, field);
      reset.hidden = false;
    });
    reset.addEventListener('click', async () => {
      await save(e, 'skin', row, field);
      reset.hidden = true;
      setTimeout(loadSettings, 300); // the field shows the skin's colour again
    });
    control.append(field, reset);
  } else if (e.type === 'skin') {
    control = skinTiles(e);
    row.classList.add('set-row-wide');
  } else if (e.run) {
    control = document.createElement('button');
    control.type = 'button';
    control.textContent = e.place ? L.moveUp : L.run;
    control.addEventListener('click', () => runAction(e, row));
  }
  if (control) {
    control.id = id;
    if (e.key) control.dataset.key = e.key;
    row.append(control);
  } else if (e.type === 'action') {
    row.append(para(e.info || L.tvOnly, e.info ? 'set-info' : 'muted'));
  }
  if (e.run && e.info) label.append(para(`${L.status}: ${e.info}`, 'muted'));
  return row;
}

/** The skins as tiles in their own colours; a click picks one and clears own changes, as on the TV. */
function skinTiles(e) {
  const box = document.createElement('div');
  box.className = 'skins';
  for (const [id, name, bg, surface, accent, text] of e.options) {
    const b = document.createElement('button');
    b.type = 'button';
    b.className = 'skin' + (id === e.value ? ' on' : '');
    b.style.background = bg;
    b.style.color = text;
    b.setAttribute('aria-pressed', String(id === e.value));
    const card = document.createElement('span');
    card.className = 'skin-card';
    card.style.background = surface;
    const dot = document.createElement('span');
    dot.className = 'skin-accent';
    dot.style.background = accent;
    const label = document.createElement('span');
    label.className = 'skin-name';
    label.textContent = name;
    b.append(card, dot, label);
    b.addEventListener('click', async () => {
      try {
        const res = await postJson('/api/design', { op: 'pick', id });
        if (res.ok) loadSettings();
      } catch (err) { /* offline */ }
    });
    box.append(b);
  }
  return box;
}

/** Own skins: save the current design, delete, export as a file, import a file. */
function designCard() {
  const card = document.createElement('div');
  card.className = 'conn';
  const h = document.createElement('div');
  h.className = 'conn-title';
  h.textContent = L.designTitle;
  const skinEntry = findEntry(setTree.pages, 'pref_skin');
  const own = skinEntry ? skinEntry.options.filter(o => o[6]) : [];
  const status = para('', 'muted');
  const rows = own.map(([id, name]) => {
    const row = document.createElement('div');
    row.className = 'conn-row';
    const n = document.createElement('strong');
    n.textContent = name;
    const del = document.createElement('button');
    del.type = 'button';
    del.className = 'small';
    del.textContent = L.remove;
    del.addEventListener('click', async () => {
      if (!confirm(L.designDelete(name))) return;
      try { await postJson('/api/design', { op: 'delete', id }); loadSettings(); } catch (err) { /* offline */ }
    });
    row.append(n, del);
    return row;
  });
  const saveRow = document.createElement('div');
  saveRow.className = 'conn-row';
  const nameField = document.createElement('input');
  nameField.maxLength = 40;
  nameField.placeholder = L.designName;
  const saveBtn = document.createElement('button');
  saveBtn.type = 'button';
  saveBtn.textContent = L.designSave;
  saveBtn.addEventListener('click', async () => {
    const name = nameField.value.trim();
    if (!name) { nameField.focus(); return; }
    try {
      const res = await postJson('/api/design', { op: 'save', name });
      if (res.ok) { status.textContent = L.designSaved(name); loadSettings(); } else status.textContent = res.error === 'full' ? L.designFull : L.notSaved;
    } catch (err) { status.textContent = L.offline; }
  });
  saveRow.append(nameField, saveBtn);
  const fileRow = document.createElement('div');
  fileRow.className = 'conn-row';
  const exp = document.createElement('button');
  exp.type = 'button';
  exp.className = 'small';
  exp.textContent = L.designExport;
  exp.addEventListener('click', async () => {
    try {
      const name = nameField.value.trim() || 'ShrugTV';
      const res = await postJson('/api/design', { op: 'export', name });
      if (!res.ok) return;
      const a = document.createElement('a');
      a.href = URL.createObjectURL(new Blob([res.json], { type: 'application/json' }));
      a.download = 'shrugtv-design-' + name.replace(/[^\w-]+/g, '_') + '.json';
      a.click();
      setTimeout(() => URL.revokeObjectURL(a.href), 1000);
    } catch (err) { status.textContent = L.offline; }
  });
  const file = document.createElement('input');
  file.type = 'file';
  file.accept = '.json,application/json';
  file.hidden = true;
  const imp = document.createElement('button');
  imp.type = 'button';
  imp.className = 'small';
  imp.textContent = L.designImport;
  imp.addEventListener('click', () => file.click());
  file.addEventListener('change', async () => {
    const f = file.files && file.files[0];
    if (!f) return;
    try {
      const res = await postJson('/api/design', { op: 'import', json: await f.text() });
      if (res.ok) { status.textContent = L.designImported(res.name); loadSettings(); }
      else status.textContent = res.error === 'full' ? L.designFull : L.designBad;
    } catch (err) { status.textContent = L.offline; }
    file.value = '';
  });
  fileRow.append(exp, imp, file);
  card.append(h, ...(rows.length ? rows : [para(L.designNone, 'muted')]), saveRow, fileRow, status);
  return card;
}

function findEntry(list, key) {
  for (const e of list || []) {
    if (e.key === key) return e;
    const hit = findEntry(e.children, key);
    if (hit) return hit;
  }
  return null;
}

/** (i) button that shows or hides a guide. */
function infoButton(box) {
  const b = document.createElement('button');
  b.type = 'button';
  b.className = 'info';
  b.textContent = 'i';
  b.setAttribute('aria-label', L.guideShow);
  b.title = L.guideShow;
  b.setAttribute('aria-expanded', 'false');
  b.setAttribute('aria-controls', box.id);
  b.addEventListener('click', ev => {
    ev.preventDefault();
    ev.stopPropagation(); // inside a <summary> it must not fold the section
    box.hidden = !box.hidden;
    b.setAttribute('aria-expanded', String(!box.hidden));
  });
  return b;
}

function toggleText(e) {
  if (e.type === 'toggle' && !e.text) return setValues[e.key] ? (e.textOn || '') : (e.textOff || '');
  return e.text || '';
}

// Rows whose switch (android:dependency) is off are greyed out, as on the TV
function updateActive() {
  for (const el of document.querySelectorAll('#set-content [data-dep]')) el.classList.toggle('inactive', !setValues[el.dataset.dep]);
  for (const c of document.querySelectorAll('#set-content .set-row > input, #set-content .set-row > select, #set-content .set-row > button')) {
    c.disabled = !!c.closest('.inactive');
  }
}

function flash(row, text, bad) {
  const old = row.querySelector('.flash');
  if (old) old.remove();
  const s = document.createElement('span');
  s.className = 'flash' + (bad ? ' error' : '');
  s.textContent = text;
  row.querySelector('.set-head').append(s);
  setTimeout(() => s.remove(), 2500);
}

async function postJson(path, body) {
  const r = await fetch(path, { method: 'POST', credentials: 'same-origin', headers: { 'Content-Type': 'application/json; charset=utf-8' }, body: JSON.stringify(body) });
  if (r.status === 401) { showPair(''); throw new Error('unpaired'); }
  return r.json();
}

/** Puts the stored value back into the control after a refused or failed change. */
function revert(e, control) {
  if (e.type === 'toggle') control.checked = !!setValues[e.key];
  else if (e.type === 'list') control.value = e.value;
  else if (e.type === 'number') control.value = e.value;
  else if (e.type === 'text') control.value = e.secret ? '' : (e.value || '');
}

async function save(e, value, row, control) {
  ownKeys.set(e.key, Date.now());
  try {
    const res = await postJson('/api/settings', { values: { [e.key]: value } });
    if (res.stored) {
      setValues[e.key] = value;
      if (e.type === 'list' || e.type === 'number') e.value = value; else if (e.type === 'text' && !e.secret) e.value = value;
      if (e.secret) { e.isSet = value !== ''; control.value = ''; control.placeholder = e.isSet ? L.secretSet : L.secretEmpty; }
      if (e.type === 'toggle' && !e.text) {
        const p = row.querySelector('.desc');
        const t = toggleText(e);
        if (p) p.textContent = t; else if (t) row.querySelector('.set-label').append(para(t, 'muted desc'));
      }
      flash(row, L.saved);
      updateActive();
    } else {
      revert(e, control);
      flash(row, res.error === 'disabled' ? L.settingsOff : L.notSaved, true);
    }
  } catch (err) {
    revert(e, control);
    flash(row, L.offline, true);
  }
}

async function runAction(e, row) {
  try {
    const res = await postJson('/api/settings/action', { key: e.key });
    if (res.ok) {
      flash(row, res.message ? `${L.status}: ${res.message}` : L.done);
      // The home rows list is sorted by place: load it again
      if (e.place || e.key === 'pref_row_order_reset') setTimeout(loadSettings, 300);
    } else {
      flash(row, res.error === 'no_screen' ? L.noScreen : L.tvOnly, true);
    }
  } catch (err) { flash(row, L.offline, true); }
}

$('set-search').addEventListener('input', () => { if (setTree) { renderGroups(); renderSettings(); } });

// Backup: every value except secrets, MQTT and companion (those belong to this TV), as a JSON file
$('set-export').addEventListener('click', () => {
  if (!setTree) return;
  const settings = {};
  for (const [e] of allEntries()) {
    if (e.key && !e.secret && !e.noBackup && e.value !== undefined) settings[e.key] = e.key in setValues ? setValues[e.key] : e.value;
  }
  const data = { app: 'ShrugTV', version: setTree.version, exported: new Date().toISOString(), settings };
  const url = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = `shrugtv-settings-${new Date().toISOString().slice(0, 10)}.json`;
  document.body.append(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
  $('set-status').textContent = L.exported(Object.keys(settings).length);
});

$('set-import').addEventListener('change', async ev => {
  const file = ev.target.files[0];
  ev.target.value = '';
  if (!file) return;
  let values = null;
  try {
    const data = JSON.parse(await file.text());
    if (data && data.app === 'ShrugTV' && data.settings && typeof data.settings === 'object') values = data.settings;
  } catch (e) { values = null; }
  if (!values) { $('set-status').textContent = L.importBad; return; }
  const clean = {};
  for (const [k, v] of Object.entries(values)) if (typeof v === 'boolean' || typeof v === 'string') clean[k] = v;
  for (const k of Object.keys(clean)) ownKeys.set(k, Date.now());
  try {
    const res = await postJson('/api/settings', { values: clean, backup: true });
    $('set-status').textContent = L.imported(res.stored || 0, (res.rejected || []).length + (res.skipped || []).length);
    loadSettings();
  } catch (e) { $('set-status').textContent = L.offline; }
});

// ---------------------------------------------------------------- log (like Glasshouse's Tools tab)
const LOG_MAX = 5000;
let logLines = [];      // [seq, time, level, tag, msg, pid, tid]
let logLast = 0;
let logTimer = null;
let logLevel = 'all';
let logPaused = false;
let logInfo = {};
let logOpen = null;     // seq of the line whose details are open

const levelOf = l => (l === 'F' || l === 'A') ? 'E' : l === 'V' ? 'D' : l;

function logStart() {
  logPoll();
}

function logStop() {
  clearTimeout(logTimer);
  logTimer = null;
}

async function logPoll() {
  clearTimeout(logTimer);
  if (currentTab !== 'log') return;
  // Fetch only what is new; nothing while paused or while the browser tab is hidden
  if (!logPaused && !document.hidden) {
    try {
      const r = await fetch(`/api/log?after=${logLast}`, { credentials: 'same-origin' });
      if (r.status === 401) { showPair(''); return; }
      const off = r.status === 403;
      $('log-off').hidden = !off;
      $('log-ui').hidden = off;
      if (!off) {
        const d = await r.json();
        logInfo = d;
        if (d.lines.length) {
          logLines = logLines.concat(d.lines).slice(-LOG_MAX);
          logLast = d.lines[d.lines.length - 1][0];
          logAppend(d.lines);
        } else if (!$('log-view').childElementCount) {
          logRender();
        }
      }
    } catch (e) { /* offline: the banner says so */ }
  }
  logTimer = setTimeout(logPoll, 2000);
}
document.addEventListener('visibilitychange', () => { if (!document.hidden && currentTab === 'log') logPoll(); });

function logMatches(row) {
  const [, , level, tag, msg] = row;
  if (logLevel !== 'all' && levelOf(level) !== logLevel) return false;
  const src = $('log-tag').value;
  if (src && tag !== src) return false;
  const q = $('log-search').value.trim().toLowerCase();
  return !q || msg.toLowerCase().includes(q) || tag.toLowerCase().includes(q);
}

const pad = (n, w) => String(n).padStart(w || 2, '0');
function logTime(t, withDate) {
  const d = new Date(t);
  const time = `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${pad(d.getMilliseconds(), 3)}`;
  return withDate ? `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${time}` : time;
}

/** Text with the search term marked (text nodes only, never HTML). */
function marked(text, q) {
  if (!q) return [document.createTextNode(text)];
  const out = [];
  const low = text.toLowerCase();
  let pos = 0;
  let i = low.indexOf(q);
  while (i >= 0) {
    if (i > pos) out.push(document.createTextNode(text.slice(pos, i)));
    const m = document.createElement('mark');
    m.textContent = text.slice(i, i + q.length);
    out.push(m);
    pos = i + q.length;
    i = low.indexOf(q, pos);
  }
  if (pos < text.length) out.push(document.createTextNode(text.slice(pos)));
  return out;
}

function logRow(row) {
  const [seq, t, level, tag, msg] = row;
  const q = $('log-search').value.trim().toLowerCase();
  const div = document.createElement('div');
  div.className = 'll lv-' + levelOf(level);
  div.dataset.seq = seq;
  const time = document.createElement('span');
  time.className = 'lt';
  time.textContent = logTime(t);
  const lv = document.createElement('span');
  lv.className = 'lv';
  lv.textContent = level;
  const tg = document.createElement('span');
  tg.className = 'lg';
  tg.append(...marked(tag, q));
  const m = document.createElement('span');
  m.className = 'lm';
  m.append(...marked(msg, q));
  div.append(time, lv, tg, m);
  return div;
}

/** Inspector under a line: all fields, JSON in the message pretty-printed. */
function logDetails(row) {
  const [seq, t, level, tag, msg, pid, tid] = row;
  const box = document.createElement('div');
  box.className = 'ld';
  box.dataset.for = seq;
  const dl = document.createElement('dl');
  for (const [k, v] of [[L.logTime, logTime(t, true)], [L.logLevel, level], [L.logTag, tag], [L.logThread, `${pid} / ${tid}`]]) {
    const dt = document.createElement('dt');
    dt.textContent = k;
    const dd = document.createElement('dd');
    dd.textContent = v;
    dl.append(dt, dd);
  }
  const pre = document.createElement('pre');
  const start = msg.search(/[{[]/);
  let pretty = null;
  if (start >= 0) {
    try { pretty = msg.slice(0, start) + '\n' + JSON.stringify(JSON.parse(msg.slice(start)), null, 2); } catch (e) { pretty = null; }
  }
  pre.textContent = pretty || msg;
  box.append(dl, pre);
  return box;
}

function atBottom(el) {
  return el.scrollHeight - el.scrollTop - el.clientHeight < 30;
}

function logRender() {
  const view = $('log-view');
  const shown = logLines.filter(logMatches);
  const frag = document.createDocumentFragment();
  for (const row of shown) {
    frag.append(logRow(row));
    if (logOpen === row[0]) frag.append(logDetails(row));
  }
  view.replaceChildren(frag);
  if (!shown.length) view.append(para(logLines.length ? L.logEmpty : '…', 'muted'));
  view.scrollTop = view.scrollHeight;
  $('log-latest').hidden = true;
  logCounts();
}

function logAppend(rows) {
  const view = $('log-view');
  const follow = atBottom(view) || !view.querySelector('.ll');
  if (!view.querySelector('.ll')) view.replaceChildren();
  const frag = document.createDocumentFragment();
  for (const row of rows) if (logMatches(row)) frag.append(logRow(row));
  view.append(frag);
  // Keep the DOM as long as the buffer
  let extra = view.querySelectorAll('.ll').length - LOG_MAX;
  while (extra-- > 0) {
    const first = view.querySelector('.ll');
    if (first.nextElementSibling && first.nextElementSibling.classList.contains('ld')) first.nextElementSibling.remove();
    first.remove();
  }
  if (follow) view.scrollTop = view.scrollHeight; else $('log-latest').hidden = false;
  logCounts();
}

function logCounts() {
  const n = { E: 0, W: 0 };
  const tags = new Set();
  for (const row of logLines) {
    const l = levelOf(row[2]);
    if (l in n) n[l]++;
    tags.add(row[3]);
  }
  $('n-E').textContent = n.E || '';
  $('n-W').textContent = n.W || '';
  const sel = $('log-tag');
  const current = sel.value;
  const list = [...tags].sort((a, b) => a.localeCompare(b));
  if (sel.dataset.tags !== list.join('|')) {
    sel.replaceChildren(new Option(L.logAllSources, ''), ...list.map(t => new Option(t, t)));
    sel.dataset.tags = list.join('|');
    sel.value = list.includes(current) ? current : '';
  }
  const shown = $('log-view').querySelectorAll('.ll').length;
  $('log-status').textContent = L.logShown(shown, logLines.length) + (logPaused ? ` · ${L.logPaused}` : '') +
    (logInfo.version ? ` · ShrugTV ${logInfo.version} · ${logInfo.device || ''}` : '');
}

$('log-view').addEventListener('scroll', () => { if (atBottom($('log-view'))) $('log-latest').hidden = true; });
$('log-latest').addEventListener('click', () => {
  $('log-view').scrollTop = $('log-view').scrollHeight;
  $('log-latest').hidden = true;
});
$('log-view').addEventListener('click', ev => {
  const line = ev.target.closest('.ll');
  if (!line) return;
  const seq = Number(line.dataset.seq);
  const open = $('log-view').querySelector('.ld');
  if (open) open.remove();
  if (logOpen === seq) { logOpen = null; return; }
  logOpen = seq;
  const row = logLines.find(r => r[0] === seq);
  if (row) line.after(logDetails(row));
});
for (const b of document.querySelectorAll('[data-level]')) {
  b.addEventListener('click', () => {
    logLevel = b.dataset.level;
    for (const o of document.querySelectorAll('[data-level]')) o.setAttribute('aria-pressed', String(o === b));
    logRender();
  });
}
$('log-search').addEventListener('input', logRender);
$('log-tag').addEventListener('change', logRender);
$('log-pause').addEventListener('click', () => {
  logPaused = !logPaused;
  $('log-pause').setAttribute('aria-pressed', String(logPaused));
  $('log-pause').textContent = logPaused ? L.logResume : L.logPause;
  logCounts();
  if (!logPaused) logPoll();
});

// Private details for copying and exporting (the view always shows the full log, as in Glasshouse)
const PRIVATE = [
  [/\b(?:\d{1,3}\.){3}\d{1,3}\b/g, '<ip>'],
  [/\b(?:[0-9a-f]{1,4}:){4,7}[0-9a-f]{1,4}\b/gi, '<ip6>'],
  [/\b(?:[0-9a-f]{2}[:-]){5}[0-9a-f]{2}\b/gi, '<mac>'],
  [/[\w.+-]+@[\w-]+\.[\w.-]+/g, '<email>'],
  [/\b(user_id|broadcaster_user_id|userId|device_id|serial)([=": ]+)\w+/gi, '$1$2<id>'],
  [/(SSID[=": ]+)[^,\s"]+/gi, '$1<wifi>']
];
function scrub(text) {
  return $('log-private').checked ? PRIVATE.reduce((s, [re, rep]) => s.replace(re, rep), text) : text;
}

function logText() {
  const shown = logLines.filter(logMatches);
  const head = `ShrugTV ${logInfo.version || ''} log, ${logInfo.device || ''}, exported ${logTime(Date.now(), true)}` +
    ($('log-search').value ? `, search "${$('log-search').value}"` : '') + (logLevel !== 'all' ? `, level ${logLevel}` : '') +
    ($('log-tag').value ? `, source ${$('log-tag').value}` : '');
  const body = shown.map(([, t, level, tag, msg, pid, tid]) => `${logTime(t, true)} ${pid} ${tid} ${level} ${tag}: ${msg}`).join('\n');
  return [shown.length, scrub(head + '\n' + body + '\n')];
}

$('log-copy').addEventListener('click', async () => {
  const [n, text] = logText();
  try {
    await navigator.clipboard.writeText(text);
  } catch (e) {
    // Plain HTTP: the clipboard API may be missing, fall back to a hidden text field
    const ta = document.createElement('textarea');
    ta.value = text;
    document.body.append(ta);
    ta.select();
    document.execCommand('copy');
    ta.remove();
  }
  $('log-status').textContent = L.logCopied(n);
});

$('log-export').addEventListener('click', () => {
  const [n, text] = logText();
  const d = new Date();
  const url = URL.createObjectURL(new Blob([text], { type: 'text/plain' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = `shrugtv-log-${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}-${pad(d.getHours())}${pad(d.getMinutes())}${pad(d.getSeconds())}.txt`;
  document.body.append(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
  $('log-status').textContent = L.logSaved(n);
});

start();


// ---------------------------------------------------------------- remote: touchpad, audio only, recent channels
const buzz = ms => { try { if (navigator.vibrate) navigator.vibrate(ms); } catch (e) { /* not supported */ } };

function setMode(mode) {
  const pad = mode === 'pad';
  $('pad').hidden = !pad;
  $('dpad').hidden = pad;
  $('mode-pad').setAttribute('aria-pressed', String(pad));
  $('mode-keys').setAttribute('aria-pressed', String(!pad));
  store('shrugtv.remote', JSON.stringify(mode));
}
$('mode-pad').addEventListener('click', () => setMode('pad'));
$('mode-keys').addEventListener('click', () => setMode('keys'));
setMode(load('shrugtv.remote', matchMedia('(pointer:coarse)').matches ? 'pad' : 'keys') === 'pad' ? 'pad' : 'keys');

(function padInit() {
  const el = $('pad');
  const arrow = $('pad-arrow');
  const STEP = 34; // pixels of dragging per key press, so a long swipe scrolls
  const pointers = new Map();
  let most = 0, moved = false, sx = 0, sy = 0, timer = 0;
  const flash = k => {
    arrow.textContent = { up: '▲', down: '▼', left: '◀', right: '▶', ok: '●', back: '↩' }[k] || '';
    arrow.classList.add('show');
    clearTimeout(timer);
    timer = setTimeout(() => arrow.classList.remove('show'), 60);
  };
  el.addEventListener('pointerdown', ev => {
    ev.preventDefault();
    try { el.setPointerCapture(ev.pointerId); } catch (e) { /* ignore */ }
    pointers.set(ev.pointerId, 1);
    most = Math.max(most, pointers.size);
    if (pointers.size === 1) { moved = false; sx = ev.clientX; sy = ev.clientY; }
    el.classList.add('on');
  });
  el.addEventListener('pointermove', ev => {
    if (!pointers.has(ev.pointerId) || pointers.size !== 1) return;
    const dx = ev.clientX - sx, dy = ev.clientY - sy;
    if (Math.max(Math.abs(dx), Math.abs(dy)) < STEP) return;
    moved = true;
    const k = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? 'right' : 'left') : (dy > 0 ? 'down' : 'up');
    key(k); flash(k); buzz(8);
    sx = ev.clientX; sy = ev.clientY;
  });
  const up = ev => {
    if (!pointers.delete(ev.pointerId)) return;
    if (pointers.size > 0) return;
    el.classList.remove('on');
    if (!moved && ev.type === 'pointerup') {
      const k = most >= 2 ? 'back' : 'ok';
      key(k); flash(k); buzz(k === 'back' ? 20 : 15);
    }
    most = 0;
  };
  el.addEventListener('pointerup', up);
  el.addEventListener('pointercancel', up);
  el.addEventListener('contextmenu', ev => ev.preventDefault());
  // Keyboard on a computer: arrows, Enter, Escape while the pad has the focus
  el.addEventListener('keydown', ev => {
    const k = { ArrowUp: 'up', ArrowDown: 'down', ArrowLeft: 'left', ArrowRight: 'right', Enter: 'ok', ' ': 'ok', Escape: 'back' }[ev.key];
    if (k) { ev.preventDefault(); key(k); flash(k); }
  });
})();

$('btn-audio').addEventListener('click', () => { cmd('audio_only', state.audio_only ? 'OFF' : 'ON'); buzz(10); });
$('btn-replay').addEventListener('click', () => buzz(10));
for (const b of document.querySelectorAll('.row button, .k')) b.addEventListener('click', () => buzz(8));

// Last channels, kept in this browser
let lastLogin = null;
function recentList() {
  const r = load('shrugtv.recent', []);
  return Array.isArray(r) ? r : [];
}
function rememberChannel(s) {
  if (s.login === lastLogin) return;
  lastLogin = s.login;
  const list = recentList().filter(c => c.login !== s.login);
  list.unshift({ login: s.login, name: s.channel || s.login, avatar: s.avatar_url || '' });
  store('shrugtv.recent', list.slice(0, 8));
  renderRecent();
}
function renderRecent() {
  const list = recentList().filter(c => c.login !== state.login && /^[A-Za-z0-9_]{1,25}$/.test(c.login || ''));
  $('recent-box').hidden = list.length === 0;
  $('recent').replaceChildren(...list.map(c => {
    const b = document.createElement('button');
    if (/^https:\/\//.test(c.avatar)) { const i = document.createElement('img'); i.alt = ''; i.src = c.avatar; b.append(i); }
    b.append(document.createTextNode(c.name || c.login));
    b.addEventListener('click', () => { cmd('watch', c.login); buzz(10); });
    return b;
  }));
}
renderRecent();

// ---------------------------------------------------------------- stars: favorites
const favorites = new Map(); // user id -> true/false as last set from here
function paintStar(b, id, on) {
  b.textContent = on ? '★' : '☆';
  b.setAttribute('aria-pressed', String(on));
  b.setAttribute('aria-label', on ? L.favRemove : L.favAdd);
  b.title = on ? L.favRemove : L.favAdd;
}
async function setFavorite(id, on) {
  const r = await api('/api/favorite', { method: 'POST', body: JSON.stringify({ id, on }) });
  if (!r.ok) throw new Error('favorite');
  favorites.set(id, on);
}
function starButton(id, initial) {
  const b = document.createElement('button');
  b.type = 'button';
  b.className = 'star';
  paintStar(b, id, favorites.has(id) ? favorites.get(id) : initial);
  b.addEventListener('click', async ev => {
    ev.stopPropagation();
    const on = b.getAttribute('aria-pressed') !== 'true';
    paintStar(b, id, on);
    buzz(10);
    try { await setFavorite(id, on); } catch (e) { paintStar(b, id, !on); alert(L.favFailed); }
  });
  return b;
}
function paintNowStar() {
  const b = $('now-star');
  const id = state.user_id;
  b.hidden = !id;
  if (id) paintStar(b, id, favorites.has(id) ? favorites.get(id) : !!state.favorite);
}
$('now-star').addEventListener('click', async () => {
  const id = state.user_id;
  if (!id) return;
  const on = $('now-star').getAttribute('aria-pressed') !== 'true';
  paintStar($('now-star'), id, on);
  buzz(10);
  try { await setFavorite(id, on); } catch (e) { paintStar($('now-star'), id, !on); alert(L.favFailed); }
});

// ---------------------------------------------------------------- chat: reply, emotes, quick phrases
let replyTo = null;
function setReply(name) {
  replyTo = name;
  $('reply-bar').hidden = !name;
  $('reply-text').textContent = name ? L.replyTo(name) : '';
  if (name) $('chat-input').focus();
}
$('reply-x').addEventListener('click', () => setReply(null));
$('chat').addEventListener('click', ev => {
  const msg = ev.target.closest('.msg');
  if (msg && msg.dataset.name) { setReply(msg.dataset.name); buzz(10); }
});

// Emotes seen in this chat (name -> image), most recent last
const seenEmotes = new Map();
function noteEmote(name, url) {
  if (!name || name.length > 40 || !/^https:\/\//.test(url)) return;
  seenEmotes.delete(name);
  seenEmotes.set(name, url);
  if (seenEmotes.size > 150) seenEmotes.delete(seenEmotes.keys().next().value);
  if (!$('emotes').hidden) emoteDirty = true;
}
let emoteDirty = false;
function renderEmotes() {
  const box = $('emotes');
  if (seenEmotes.size === 0) {
    const p = document.createElement('p');
    p.className = 'muted';
    p.textContent = L.noEmotes;
    box.replaceChildren(p);
    return;
  }
  box.replaceChildren(...[...seenEmotes].reverse().map(([name, url]) => {
    const b = document.createElement('button');
    b.type = 'button';
    b.title = name;
    const i = document.createElement('img');
    i.src = url;
    i.alt = name;
    b.append(i);
    b.addEventListener('click', () => {
      const input = $('chat-input');
      const at = input.selectionStart == null ? input.value.length : input.selectionStart;
      const before = input.value.slice(0, at), after = input.value.slice(input.selectionEnd == null ? at : input.selectionEnd);
      const pad = before && !/\s$/.test(before) ? ' ' : '';
      input.value = (before + pad + name + ' ' + after).slice(0, 500);
      const pos = (before + pad + name + ' ').length;
      input.focus();
      try { input.setSelectionRange(pos, pos); } catch (e) { /* ignore */ }
      buzz(8);
    });
    return b;
  }));
}
function showEmotes(on) {
  $('emotes').hidden = !on;
  $('emote-btn').setAttribute('aria-expanded', String(on));
  if (on) { renderEmotes(); emoteDirty = false; }
}
$('emote-btn').addEventListener('click', () => showEmotes($('emotes').hidden));
setInterval(() => { if (emoteDirty && !$('emotes').hidden) { renderEmotes(); emoteDirty = false; } }, 2000);

const QUICK_DEFAULT = ['GG', 'o/', '❤', 'LUL'];
function quickList() {
  const q = load('shrugtv.quick', null);
  return Array.isArray(q) ? q.filter(t => typeof t === 'string').slice(0, 12) : QUICK_DEFAULT.slice();
}
function renderQuick() {
  const list = quickList();
  const chips = list.map(text => {
    const b = document.createElement('button');
    b.type = 'button';
    b.textContent = text;
    let hold = 0, held = false;
    b.addEventListener('pointerdown', () => { held = false; hold = setTimeout(() => {
      held = true;
      if (confirm(L.quickRemove(text))) { store('shrugtv.quick', quickList().filter(t => t !== text)); renderQuick(); }
    }, 650); });
    for (const ev of ['pointerup', 'pointerleave', 'pointercancel']) b.addEventListener(ev, () => clearTimeout(hold));
    b.addEventListener('click', () => { if (!held && state.chat_allowed !== false) { cmd('chat_message', text); buzz(10); } });
    return b;
  });
  const add = document.createElement('button');
  add.type = 'button';
  add.className = 'add';
  add.textContent = '+';
  add.setAttribute('aria-label', L.quickAdd);
  add.addEventListener('click', () => {
    const t = (prompt(L.quickNew) || '').trim().slice(0, 60);
    if (t && !quickList().includes(t)) { store('shrugtv.quick', quickList().concat(t).slice(0, 12)); renderQuick(); }
  });
  $('quick').replaceChildren(...chips, add);
}
renderQuick();

// ---------------------------------------------------------------- version and update
let updTimer = 0;
let updInfo = null;

function updateStop() { clearInterval(updTimer); updTimer = 0; }
function updateStart() {
  updateStop();
  updateLoad();
  updTimer = setInterval(() => {
    const busy = updInfo && (updInfo.checking || ['downloading', 'verifying'].includes(updInfo.install.phase));
    if (busy || Date.now() % 30000 < 3000) updateLoad();
  }, 1500);
}
async function updateLoad() {
  try { updInfo = await api('/api/update'); } catch (e) { return; }
  updateRender();
}
async function updatePost(path) {
  try {
    updInfo = await api(path, { method: 'POST', body: '{}' });
    updateRender();
  } catch (e) { /* offline or not paired */ }
}

function fmtTime(ms) {
  if (!ms) return L.verNever;
  const d = new Date(ms);
  return d.toLocaleString(LANG, { dateStyle: 'medium', timeStyle: 'short' });
}
function mk(tag, cls, text) {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  if (text != null) e.textContent = text;
  return e;
}
function fact(dl, k, v) { dl.append(mk('dt', null, k), mk('dd', null, v)); }

function updateRender() {
  const u = updInfo;
  if (!u) return;
  document.querySelector('[data-tab=update] .new-dot').hidden = u.status !== 'available';

  const cur = mk('div', 'card');
  cur.append(mk('h2', null, L.verInstalled), mk('div', 'ver', 'ShrugTV ' + u.version));
  const dl = mk('dl', 'facts');
  fact(dl, L.verEdition, u.edition === 'public' ? L.verEditionPublic : L.verEditionPrivate);
  fact(dl, L.verDevice, u.device || '?');
  fact(dl, L.verAndroid, u.android || '?');
  if (u.check_supported) fact(dl, L.verLastCheck, fmtTime(u.last_check));
  cur.append(dl);

  const box = mk('div', 'card' + (u.status === 'available' ? ' hot' : ''));
  const btns = mk('div', 'btns');
  const inst = u.install || {};
  if (u.status === 'unsupported') {
    box.append(mk('p', 'muted', L.verUnsupported));
  } else {
    if (u.status === 'available') {
      box.append(mk('h2', null, L.verAvailable(u.latest.version)));
      if (u.latest.notes) { box.append(mk('div', 'muted', L.verNotes), mk('p', 'notes', u.latest.notes)); }
    } else {
      box.append(mk('div', 'stat', (u.status === 'current' ? '✓ ' : '') + (u.status === 'current' ? L.verCurrent : L.verUnknown)));
    }
    if (u.checking) box.append(mk('p', 'muted', L.verChecking));
    const working = ['downloading', 'verifying'].includes(inst.phase);
    if (inst.phase === 'downloading') {
      box.append(mk('p', 'stat', L.verDownloading(inst.percent)));
      const bar = mk('div', 'bar'); const i = mk('i'); i.style.width = inst.percent + '%'; bar.append(i); box.append(bar);
    } else if (inst.phase === 'verifying') {
      box.append(mk('p', 'stat', L.verVerifying));
    } else if (inst.phase === 'error') {
      const known = L.verErrors[inst.message];
      box.append(mk('p', 'error', known || inst.message));
    }
    const check = mk('button', null, u.checking ? L.verChecking : L.verCheck);
    check.type = 'button';
    check.disabled = !!u.checking || working;
    check.addEventListener('click', () => updatePost('/api/update/check'));
    if (u.status === 'available' && u.install_enabled && u.latest.installable) {
      const go = mk('button', 'primary', L.verInstall);
      go.type = 'button';
      go.disabled = working;
      go.addEventListener('click', () => { if (confirm(L.verInstallConfirm(u.latest.version))) updatePost('/api/update/install'); });
      btns.append(go);
    }
    btns.append(check);
    if (u.status === 'available' && /^https:\/\//.test(u.latest.url || '')) {
      const a = mk('a', 'btn', L.verRelease);
      a.href = u.latest.url; a.target = '_blank'; a.rel = 'noopener noreferrer';
      btns.append(a);
    }
    box.append(btns);
    if (u.status === 'available' && u.install_supported && !u.install_enabled) box.append(mk('p', 'muted', L.verInstallOff));
    if (!u.check_enabled) box.append(mk('p', 'muted', L.verAutoOff));
  }
  const home = mk('div', 'card install-hint');
  home.append(mk('h2', null, L.homeTitle), mk('div', 'muted', L.homeHint));
  const ol = mk('ol');
  ol.append(mk('li', null, L.homeIos), mk('li', null, L.homeAndroid));
  home.append(ol);
  $('upd').replaceChildren(cur, box, home);
}

// The red dot on the tab: asked once at the start (the TV answers from its last check, no network call)
function updateBadge() { api('/api/update').then(u => { updInfo = u; updateRender(); }).catch(() => {}); }


// ---------------------------------------------------------------- skin of the TV on this page
const SKIN_VARS = ['--bg', '--surface', '--surface2', '--text', '--muted', '--accent', '--accent2', '--live', '--on-accent', '--focus', '--on-focus',
  '--border', '--press', '--placeholder', '--line', '--switch-off', '--tip-bg', '--tip-text', '--hl', '--behind', '--deleted'];
const rgb = h => [1, 3, 5].map(i => parseInt(h.slice(i, i + 2), 16));
const mix = (a, b, t) => { const x = rgb(a), y = rgb(b); return '#' + x.map((v, i) => Math.round(v + (y[i] - v) * t).toString(16).padStart(2, '0')).join(''); };
const alpha = (h, t) => `rgba(${rgb(h).join(',')},${t})`;
let skinOn = false;

function applySkin(s) {
  const root = document.documentElement;
  const valid = s && s.on && s.colors && Object.values(s.colors).every(v => /^#[0-9A-F]{6}$/i.test(v));
  skinOn = !!valid;
  $('theme').hidden = skinOn;
  if (!valid) {
    for (const v of SKIN_VARS) root.style.removeProperty(v);
    let t = 'dark';
    try { if (localStorage.getItem('shrugtv.theme') === 'light') t = 'light'; } catch (e) { /* private mode */ }
    applyTheme(t);
    return;
  }
  const c = s.colors;
  const set = {
    '--bg': c.background, '--surface': c.surface, '--surface2': c.surfaceHigh, '--text': c.text, '--muted': c.textMuted,
    '--accent': c.accent, '--accent2': c.accentLight, '--live': c.live, '--on-accent': c.onAccent, '--focus': c.text, '--on-focus': c.background,
    '--border': mix(c.surfaceHigh, c.text, 0.12), '--press': mix(c.surfaceHigh, c.text, 0.12), '--placeholder': c.surfaceHigh,
    '--line': mix(c.surface, c.text, 0.10), '--switch-off': mix(c.surfaceHigh, c.text, 0.22), '--tip-bg': alpha(c.accent, 0.14),
    '--tip-text': c.accentLight, '--hl': alpha(c.accent, 0.2), '--behind': mix(c.surfaceHigh, c.text, 0.3), '--deleted': c.textMuted
  };
  for (const [k, v] of Object.entries(set)) root.style.setProperty(k, v);
  root.dataset.theme = s.dark ? 'dark' : 'light';
  document.querySelector('meta[name=theme-color]').content = c.background;
}
async function loadSkin() {
  try { applySkin(await api('/api/theme')); } catch (e) { /* offline or not paired */ }
}
