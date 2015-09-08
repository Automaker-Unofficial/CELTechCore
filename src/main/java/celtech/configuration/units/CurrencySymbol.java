package celtech.configuration.units;

/**
 *
 * @author Ian
 */
public enum CurrencySymbol
{

    POUND("£"),
    DOLLAR("$"),
    EURO("€"),
    YEN_YUAN("¥"),
    KOREAN_WON("₩"),
    KRONA("kr"),
    INDIAN_RUPEE("₹"),
    BAHT("฿");

    private final String currencySymbol;

    private CurrencySymbol(String currencySymbol)
    {
        this.currencySymbol = currencySymbol;
    }

    public String getDisplayString()
    {
        return currencySymbol;
    }
}
