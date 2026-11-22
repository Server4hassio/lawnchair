package app.lawnchair.deck

/**
 * The home screen layout Deck builds on first use, copied from the owner's
 * previous launcher.
 *
 * Apps are matched by their label. A label ending in `*` matches any app whose
 * label starts with the text before it (for names that were cut off on the old
 * home screen). Apps that are installed but not listed here are placed after
 * the last page, sorted by name.
 */
object PresetLayout {

    sealed interface Slot
    data class App(val label: String) : Slot
    data class Folder(val title: String, val apps: List<String>) : Slot

    private fun a(label: String) = App(label)
    private fun f(title: String, vararg apps: String) = Folder(title, apps.toList())

    /** Dock, left to right. */
    val hotseat: List<Slot?> = listOf(
        a("Phone"), a("Messages"), a("VIOT"), a("Chrome"), a("Camera"),
    )

    /** Pages, each a list of rows (top to bottom), each row left to right. */
    val pages: List<List<List<Slot?>>> = listOf(
        // Page 1: the top rows are left free for widgets.
        listOf(
            listOf(),
            listOf(),
            listOf(),
            listOf(),
            listOf(a("Gallery"), a("Calendar"), a("Mibro Fit"), a("Translate"), a("Play Store")),
            listOf(
                f(
                    "Google",
                    "Google", "Drive", "Google One", "Assistant", "Find Hub", "Contacts",
                    "Lens", "Safety", "Meet", "Home", "Photos",
                ),
                a("YouTube"), a("Instagram"), a("Messenger"), a("Facebook"),
            ),
        ),
        // Page 2
        listOf(
            listOf(a("File Manager"), a("Notes"), a("GSign"), a("Clock"), a("Weather")),
            listOf(
                a("Turbo VPN"),
                f(
                    "Tools",
                    "Recorder", "Scanner", "Downloads", "Compass", "Services & feedback*",
                    "Mi Remote", "MOBICOM", "ShareMe", "WPS Office", "Link to Windows*",
                    "Цахилгаан хэ*", "Cleaner",
                ),
                f(
                    "Personalization",
                    "Themes", "Game Centre", "Security", "Mi Browser", "Mi Video", "Music",
                    "Messaging",
                ),
                f("Providers", "mobicom", "Unitel", "ONDO"),
                a("e-barimt"),
            ),
            listOf(
                f("Automation", "BroadLink", "eWeLink", "Reolink", "SmartLife", "Tuya"),
                f("Social", "Telegram", "Discord", "Viber", "ClickUp", "WhatsApp", "WeChat"),
                f("Folder", "VOO", "Univision Go"),
                a("DaavkaTunes"),
                a("Home Assista*"),
            ),
            listOf(a("E-Mongolia"), a("SmartCar"), a("Zedge"), a("Spotify"), a("UB Smart Bus")),
            listOf(
                f("Maps", "what3words", "MAPS.ME"),
                f(
                    "Folder",
                    "IH Wallet", "Coinhub", "trade", "Binance", "Authenticator", "CoinMarketCap",
                    "XM", "xmeta", "TradingView", "MetaTrader 5",
                ),
                a("VIOT"),
                f(
                    "Bank",
                    "Simple", "Khan bank", "Storepay", "Golomt Bank", "hipay", "Socialpay",
                    "Simple", "SG Pay", "Toki", "OmniWay", "XacBank", "Wallet", "monpay", "TDB",
                ),
                a("Calculator"),
            ),
            listOf(
                f("E mail", "Yahoo Mail", "Gmail"),
                a("Unegui.mn"), a("Settings"), a("Tor Browser"), a("Maps"),
            ),
        ),
        // Page 3
        listOf(
            listOf(a("Zoom"), a("CapCut"), a("Darts Scorebo*"), a("UBCab"), a("Amazon Alexa")),
            listOf(a("Mobile Legen*"), a("EspTouch"), a("Cubic Air"), null, a("Net Analyzer")),
            listOf(a("Steam"), a("Mgrs & Utm*"), a("Zello"), a("Shoppy"), a("TeamViewer")),
            listOf(a("Milesight Tool*"), a("GS25mn"), a("ntfy"), a("Их н*"), a("ChatGPT")),
            listOf(
                f("Skooter", "Eco Bike", "U-GO", "Jet", "Tapa"),
                a("MT Manager"), a("Xiaomi Home"), a("AnyDesk"), a("Olula"),
            ),
            listOf(a("Claude"), a("MixerBox BFF"), a("VIOT TM")),
        ),
    )
}
