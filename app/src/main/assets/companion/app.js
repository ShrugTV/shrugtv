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
    run: 'Run', done: 'Done', tvOnly: 'Only on the TV', noScreen: 'Open ShrugTV on the TV first.', noResults: 'No settings found.',
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
    chatOff: 'Chat-Schreiben vom Handy ist in den Einstellungen des TVs ausgeschaltet.', search: 'Suchen', liveNow: 'Jetzt live',
    results: 'Ergebnisse', quality: 'Qualität', chatLayout: 'Chat-Layout', layoutSide: 'Neben dem Video',
    layoutOverlay: 'Über dem Video', layoutTransparent: 'Transparent', chatOn: 'Chat an', chatOffBtn: 'Chat aus',
    stats: 'Statistik', close: 'Player schließen', openTwitch: 'Auf Twitch öffnen', nothing: 'Es läuft nichts',
    viewers: n => `${n.toLocaleString('de-DE')} Zuschauer`, since: m => `seit ${Math.floor(m / 60)}:${String(m % 60).padStart(2, '0')} h`,
    behind: s => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')} zurück`, removed: 'Nachricht entfernt', offlineCh: 'offline',
    tabSettings: 'Einstellungen', tabRemoteShort: 'Steuern', tabChatShort: 'Chat', tabFindShort: 'Suchen', tabPlayerShort: 'Player', tabSettingsShort: 'Optionen',
    searchChannels: 'Kanalname', tabLog: 'Log',
    logOff: 'Das Log im Browser ist am TV ausgeschaltet (Einstellungen → Handy & Browser → Log im Browser).',
    logAbout: 'Das eigene Log von ShrugTV auf diesem TV, live. Tokens und Passwörter werden immer entfernt. Zeile anklicken für Details.',
    logSearch: 'Log durchsuchen', logSource: 'Quelle', logAllSources: 'Alle Quellen', logLevel: 'Stufe',
    logAll: 'Alle', logErrors: 'Fehler', logWarnings: 'Warnungen', logInfo: 'Info', logDebug: 'Debug',
    logPause: 'Pause', logResume: 'Weiter', logCopy: 'Kopieren', logExport: 'Als .txt sichern', logPrivate: 'Private Details ausblenden',
    logLatest: 'Zum Neuesten ↓', logCopied: n => `${n} Zeilen kopiert`, logSaved: n => `${n} Zeilen gesichert`,
    logShown: (n, total) => `${n} von ${total} Zeilen`, logEmpty: 'Keine passenden Zeilen.', logPaused: 'pausiert',
    logTime: 'Zeit', logTag: 'Quelle', logThread: 'Prozess/Thread', logMessage: 'Nachricht', settingsOff: 'Einstellungen vom Browser sind am TV ausgeschaltet (Einstellungen → Handy & Browser).',
    settingsSearch: 'Einstellungen suchen', export: 'In Datei sichern', import: 'Aus Datei laden', saved: 'Gespeichert', notSaved: 'Nicht gespeichert: Wert nicht erlaubt',
    imported: (n, m) => `${n} Einstellungen geladen` + (m ? `, ${m} übersprungen` : ''), importBad: 'Das ist keine ShrugTV-Einstellungsdatei.',
    run: 'Ausführen', done: 'Erledigt', tvOnly: 'Nur am TV', noScreen: 'Öffne zuerst ShrugTV am TV.', noResults: 'Keine Einstellungen gefunden.',
    secretSet: 'gespeichert – tippen zum Ersetzen', secretEmpty: 'nicht gesetzt', on: 'An', off: 'Aus', status: 'Status',
    pinned: 'Angeheftet', pin: 'An die Seitenleiste heften', unpin: 'Lösen', noPins: 'Hefte Einstellungen mit ☆ an, um sie hier zu finden.',
    exported: n => `${n} Einstellungen in die Datei gesichert (ohne MQTT und Companion, die gehören zu diesem TV).`,
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
    run: 'Ejecutar', done: 'Listo', tvOnly: 'Solo en el TV', noScreen: 'Abre primero ShrugTV en el TV.', noResults: 'No se encontraron ajustes.',
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
    mqttStates: { connected: 'conectado', connecting: 'conectando …', off: 'desactivado', 'no broker address': 'sin dirección de broker' }
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
    run: 'Exécuter', done: 'Terminé', tvOnly: 'Uniquement sur la TV', noScreen: 'Ouvrez d\'abord ShrugTV sur la TV.', noResults: 'Aucun réglage trouvé.',
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
    mqttStates: { connected: 'connecté', connecting: 'connexion …', off: 'désactivé', 'no broker address': 'pas d\'adresse de broker' }
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
    run: 'Executar', done: 'Concluído', tvOnly: 'Só na TV', noScreen: 'Abra o ShrugTV na TV primeiro.', noResults: 'Nenhuma configuração encontrada.',
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
    mqttStates: { connected: 'conectado', connecting: 'conectando …', off: 'desligado', 'no broker address': 'sem endereço do broker' }
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
    run: 'Выполнить', done: 'Готово', tvOnly: 'Только на телевизоре', noScreen: 'Сначала откройте ShrugTV на телевизоре.', noResults: 'Настройки не найдены.',
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
    mqttStates: { connected: 'подключено', connecting: 'подключение …', off: 'выкл.', 'no broker address': 'нет адреса брокера' }
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
    run: 'Çalıştır', done: 'Bitti', tvOnly: 'Yalnızca TV\'de', noScreen: 'Önce TV\'de ShrugTV\'yi açın.', noResults: 'Ayar bulunamadı.',
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
    mqttStates: { connected: 'bağlı', connecting: 'bağlanıyor …', off: 'kapalı', 'no broker address': 'aracı adresi yok' }
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
    run: 'Esegui', done: 'Fatto', tvOnly: 'Solo sul TV', noScreen: 'Apri prima ShrugTV sul TV.', noResults: 'Nessuna impostazione trovata.',
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
    mqttStates: { connected: 'connesso', connecting: 'connessione …', off: 'disattivato', 'no broker address': 'nessun indirizzo del broker' }
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
    run: '実行', done: '完了', tvOnly: 'テレビでのみ', noScreen: '先にテレビでShrugTVを開いてください。', noResults: '設定が見つかりません。',
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
    mqttStates: { connected: '接続済み', connecting: '接続中…', off: 'オフ', 'no broker address': 'ブローカーのアドレスなし' }
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
    run: '실행', done: '완료', tvOnly: 'TV에서만 가능', noScreen: '먼저 TV에서 ShrugTV를 여세요.', noResults: '설정을 찾을 수 없어요.',
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
    mqttStates: { connected: '연결됨', connecting: '연결 중 …', off: '꺼짐', 'no broker address': '브로커 주소 없음' }
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
    run: '运行', done: '完成', tvOnly: '仅限电视', noScreen: '请先在电视上打开 ShrugTV。', noResults: '未找到设置。',
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
    mqttStates: { connected: '已连接', connecting: '连接中 …', off: '关', 'no broker address': '无代理地址' }
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
    run: 'Uruchom', done: 'Gotowe', tvOnly: 'Tylko na telewizorze', noScreen: 'Najpierw otwórz ShrugTV na telewizorze.', noResults: 'Nie znaleziono ustawień.',
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
    mqttStates: { connected: 'połączono', connecting: 'łączenie …', off: 'wyłączone', 'no broker address': 'brak adresu brokera' }
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
const L = T[LANG];
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
    else if (msg.t === 'settings') settingsChanged(msg.d.key);
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
    if (l.r) {
      const r = document.createElement('span');
      r.className = 'r';
      r.textContent = '↪ ' + l.r;
      div.append(r);
    }
    if (l.n && !l.no) {
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
  const text = $('chat-input').value.trim();
  if (text) cmd('chat_message', text);
  $('chat-input').value = '';
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
    name.textContent = (it.favorite ? '★ ' : '') + (it.name || it.login) + (it.live === false ? ` · ${L.offlineCh}` : '');
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
    return li;
  }));
}

async function loadLive() {
  try { renderCards((await api('/api/live')).items, L.liveNow); } catch (e) { /* not paired or offline */ }
}

$('search-form').addEventListener('submit', async ev => {
  ev.preventDefault();
  const q = $('search').value.trim();
  if (!q) { loadLive(); return; }
  try { renderCards((await api('/api/search?q=' + encodeURIComponent(q))).items, L.results); } catch (e) { /* ignore */ }
});

// ---------------------------------------------------------------- tabs, deep links and controls
// Every tab and settings group has its own address: #remote, #chat, #find, #player, #settings,
// #settings/<group key>, #settings/pinned (bookmarkable, like Glasshouse's ?tab=…)
const TABS = ['remote', 'chat', 'find', 'player', 'settings', 'log'];
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
const MQTT_GROUPS = { pref_mqtt_entities_info: 'info', pref_mqtt_entities_avatar: 'avatar', pref_mqtt_entities_preview: 'preview', pref_mqtt_entities_events: 'events', pref_mqtt_commands: 'controls' };
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
  title.textContent = e.title;
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
    control.textContent = L.run;
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
    if (res.ok) flash(row, res.message ? `${L.status}: ${res.message}` : L.done);
    else flash(row, res.error === 'no_screen' ? L.noScreen : L.tvOnly, true);
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
