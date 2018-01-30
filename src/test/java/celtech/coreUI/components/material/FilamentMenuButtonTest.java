package celtech.coreUI.components.material;

import celtech.JavaFXConfiguredTest;
import celtech.roboxbase.configuration.Filament;
import celtech.roboxbase.configuration.datafileaccessors.FilamentContainer;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author Ian
 */
public class FilamentMenuButtonTest extends JavaFXConfiguredTest
{

    @Before
    public void filamentPre()
    {
        System.err.println("In FilamentMenuButtonTest.filamentPre()");    
    }
    
    @Test
    public void testCategoryComparator()
    {
        System.err.println("In FilamentMenuButtonTest::testCategoryComparator");
        Filament roboxCategory = FilamentContainer.UNKNOWN_FILAMENT.clone();
        roboxCategory.setCategory("Robox");
        Filament aCategory = FilamentContainer.UNKNOWN_FILAMENT.clone();
        aCategory.setCategory("A Category");
        Filament zCategory = FilamentContainer.UNKNOWN_FILAMENT.clone();
        zCategory.setCategory("Z Category");
        Filament customCategory = FilamentContainer.UNKNOWN_FILAMENT.clone();
        customCategory.setCategory("Custom");

        System.err.println("    assertions");
        //Basic alpha sort check
        assertTrue(FilamentMenuButton.byCategory.compare(aCategory, zCategory) < 0);
        assertTrue(FilamentMenuButton.byCategory.compare(aCategory, aCategory) == 0);
        assertTrue(FilamentMenuButton.byCategory.compare(zCategory, aCategory) > 0);
        System.err.println("    robox first");
        //Check Robox always comes first
        assertTrue(FilamentMenuButton.byCategory.compare(roboxCategory, zCategory) < 0);
        assertTrue(FilamentMenuButton.byCategory.compare(roboxCategory, aCategory) < 0);
        assertTrue(FilamentMenuButton.byCategory.compare(zCategory, roboxCategory) > 0);
        assertTrue(FilamentMenuButton.byCategory.compare(aCategory, roboxCategory) > 0);
        assertTrue(FilamentMenuButton.byCategory.compare(customCategory, roboxCategory) > 0);
        assertTrue(FilamentMenuButton.byCategory.compare(roboxCategory, roboxCategory) == 0);
        //Check Custom always comes last
        System.err.println("    custom last");
        assertTrue(FilamentMenuButton.byCategory.compare(customCategory, zCategory) > 0);
        assertTrue(FilamentMenuButton.byCategory.compare(customCategory, aCategory) > 0);
        assertTrue(FilamentMenuButton.byCategory.compare(zCategory, customCategory) < 0);
        assertTrue(FilamentMenuButton.byCategory.compare(aCategory, customCategory) < 0);
        assertTrue(FilamentMenuButton.byCategory.compare(customCategory, customCategory) == 0);
        System.err.println("    done testCategoryComparator");
    }

    @Test
    public void testCategoryByNameComparator()
    {
        System.err.println("In FilamentMenuButtonTest::testCategoryByNameComparator");
        String roboxCategory = "Robox";
        String aCategory = "A Category";
        String zCategory = "Z Category";
        String customCategory = "Custom";

        System.err.println("    assertions");
        //Basic alpha sort check
        assertTrue(FilamentMenuButton.byBrandName.compare(aCategory, zCategory) < 0);
        assertTrue(FilamentMenuButton.byBrandName.compare(aCategory, aCategory) == 0);
        assertTrue(FilamentMenuButton.byBrandName.compare(zCategory, aCategory) > 0);

        System.err.println("    robox first");
        //Check Robox always comes first
        assertTrue(FilamentMenuButton.byBrandName.compare(roboxCategory, zCategory) < 0);
        assertTrue(FilamentMenuButton.byBrandName.compare(roboxCategory, aCategory) < 0);
        assertTrue(FilamentMenuButton.byBrandName.compare(zCategory, roboxCategory) > 0);
        assertTrue(FilamentMenuButton.byBrandName.compare(aCategory, roboxCategory) > 0);
        assertTrue(FilamentMenuButton.byBrandName.compare(customCategory, roboxCategory) > 0);
        assertTrue(FilamentMenuButton.byBrandName.compare(roboxCategory, roboxCategory) == 0);
        //Check Custom always comes last
        System.err.println("    custom last");
        assertTrue(FilamentMenuButton.byBrandName.compare(customCategory, zCategory) > 0);
        assertTrue(FilamentMenuButton.byBrandName.compare(customCategory, aCategory) > 0);
        assertTrue(FilamentMenuButton.byBrandName.compare(zCategory, customCategory) < 0);
        assertTrue(FilamentMenuButton.byBrandName.compare(aCategory, customCategory) < 0);
        assertTrue(FilamentMenuButton.byBrandName.compare(customCategory, customCategory) == 0);
        System.err.println("    done testCategoryByNameComparator");
    }
}
