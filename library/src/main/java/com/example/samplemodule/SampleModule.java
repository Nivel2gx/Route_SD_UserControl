package com.example.samplemodule;

import android.content.Context;
import android.util.Log;

import com.artech.externalapi.ExternalApiDefinition;
import com.artech.externalapi.ExternalApiFactory;
import com.artech.framework.GenexusModule;
import com.artech.usercontrols.UcFactory;
import com.artech.usercontrols.UserControlDefinition;

public class SampleModule implements GenexusModule {

	@Override
	public void initialize(Context context) {
		ExternalApiDefinition basicExternalObject = new ExternalApiDefinition(
				BasicExternalObject.NAME,
				BasicExternalObject.class
		);
		ExternalApiFactory.addApi(basicExternalObject);

		Log.e("UC:"," Antes que nada");
		UserControlDefinition basicUserControl = new UserControlDefinition(
				BasicUserControl.NAME,
				BasicUserControl.class
		);
		Log.e("UC:"," Despues de crea la instancia");
		UcFactory.addControl(basicUserControl);
	}
}
