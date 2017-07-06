/*--------------------------------------------------------------------------+
$Id: AnalysesActivator.java 5449 2012-08-24 19:28:08Z ratiu $
|                                                                          |
| Copyright 2011 ForTISS GmbH                     |
|                                                                          |
| Licensed under the Apache License, Version 2.0 (the "License");          |
| you may not use this file except in compliance with the License.         |
| You may obtain a copy of the License at                                  |
|                                                                          |
|    http://www.apache.org/licenses/LICENSE-2.0                            |
|                                                                          |
| Unless required by applicable law or agreed to in writing, software      |
| distributed under the License is distributed on an "AS IS" BASIS,        |
| WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. |
| See the License for the specific language governing permissions and      |
| limitations under the License.                                           |
+--------------------------------------------------------------------------*/
package org.opt4j.operators;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 * 
 * @author ratiu
 * @author $Author: ratiu $
 * @version $Rev: 5449 $
 * @ConQAT.Rating GREEN Hash: 26273ECC4487F5E09548132D2126639A
 */
public class Opt4JOperatorsActivator extends Plugin {

	/** The plug-in ID. */
	public static final String PLUGIN_ID = Opt4JOperatorsActivator.class.getPackage().getName(); //$NON-NLS-1$

	/** The shared instance. */
	private static Opt4JOperatorsActivator plugin;

	/** {@inheritDoc} */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/** {@inheritDoc} */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/** Returns the shared instance. */
	public static Opt4JOperatorsActivator getDefault() {
		return plugin;
	}
}
