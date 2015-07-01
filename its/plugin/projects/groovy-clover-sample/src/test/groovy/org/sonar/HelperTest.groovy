//
// Generated from archetype; please customize.
//

package org.sonar

import org.sonar.Helper
import org.sonar.Example

/**
 * Tests for the {@link Helper} class.
 */
class HelperTest
    extends GroovyTestCase
{
    void testHelp() {
        new Helper().help(new Example())
    }
}
