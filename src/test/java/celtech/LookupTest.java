/*
 * Copyright 2015 CEL UK
 */
package celtech;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author tony
 */
public class LookupTest 
{
    
    
    @ClassRule
    public static TemporaryFolder temporaryUserStorageFolder = new TemporaryFolder();
    
    @BeforeClass
    public static void setUpClass()
    {
        AutoMakerTestConfigurator.setUp(temporaryUserStorageFolder);
    }

   
    @Test
    public void testI18n()
    {
        String t01 = Lookup.i18n("*T01");
        assertEquals("Disconnect your machine from USB and AC power. Check your USB cable is connected correctly.",
                     t01);
    }

    @Test
    public void testSubstituteTemplatesSimpleSubstitution()
    {
        String t01 = Lookup.i18n("*T01");
        String e01 = Lookup.substituteTemplates("*T01");
        assertEquals(t01, e01);
    }
    
    @Test
    public void testSubstituteTemplatesDoubleSubstitution()
    {
        String t01 = Lookup.i18n("*T01");
        String e01 = Lookup.substituteTemplates("*T01*T01");
        assertEquals(t01 + t01, e01);
    }    
    
    @Test
    public void testSubstituteTemplatesDifferentSubstitutions()
    {
        String t01 = Lookup.i18n("*T01");
        String t02 = Lookup.i18n("*T02");
        String mess = Lookup.substituteTemplates("ABCD *T01*T01 EFG *T02 ZZZ");
        assertEquals("ABCD " + t01 + t01 + " EFG " + t02 + " ZZZ", mess);
    }        

   
}