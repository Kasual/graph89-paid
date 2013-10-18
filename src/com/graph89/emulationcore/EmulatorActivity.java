/*
 *   Graph89 - Emulator for Android
 *  
 *	 Copyright (C) 2012-2013  Dritan Hashorva
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.

 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.graph89.emulationcore;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.Vibrator;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;

import com.Bisha.TI89EmuDonation.R;
import com.graph89.common.BackwardCompatibility;
import com.graph89.common.CalculatorInfoTI84;
import com.graph89.common.CalculatorInfoTI89;
import com.graph89.common.CalculatorInfoV200;
import com.graph89.common.CalculatorInstance;
import com.graph89.common.CalculatorInstanceHelper;
import com.graph89.common.CalculatorTypes;
import com.graph89.common.Dimension2D;
import com.graph89.common.Directories;
import com.graph89.common.EmulatorThread;
import com.graph89.common.GoogleAccount;
import com.graph89.common.ProgressDialogControl;
import com.graph89.common.SkinBase;
import com.graph89.common.SkinDefinition;
import com.graph89.common.TI84Specific;
import com.graph89.common.TI89Specific;
import com.graph89.common.Util;
import com.graph89.common.V200Specific;
import com.graph89.controls.FilePickerActivity;
import com.graph89.controls.ReceivedFileSaver;
import com.graph89.controls.ScreenshotTaker;
import com.graph89.controls.WhatsNew;

public class EmulatorActivity extends Graph89ActivityBase
{
	// ///////////Handler Codes////////////////////////////////////////////////
	public static final int						HANDLER_STARTGALLERY			= Graph89ActivityBase.MAX_HANDLER_ID + 1;
	public static final int						HANDLER_RECEIVEFILE				= Graph89ActivityBase.MAX_HANDLER_ID + 2;
	public static final int						HANDLER_SHOWPROGRESSDIALOG		= Graph89ActivityBase.MAX_HANDLER_ID + 3;
	public static final int						HANDLER_UPDATEPROGRESSDIALOG	= Graph89ActivityBase.MAX_HANDLER_ID + 4;
	public static final int						HANDLER_HIDEPROGRESSDIALOG		= Graph89ActivityBase.MAX_HANDLER_ID + 5;
	public static final int						HANDLER_TERMINATE				= Graph89ActivityBase.MAX_HANDLER_ID + 6;
	// //////////////OnActivityResult Codes/////////////////////////////////////
	public static final int						INSTALL_APP						= 1;
	// /////////////////////////////////////////////////////////////////////////

	public static UIStateManager				UIStateManagerObj				= null;
	public static CalculatorInstance			ActiveInstance					= null;
	public static int							ActiveInstanceIndex				= 0;
	public static CalculatorInstanceHelper		CalculatorInstances				= null;

	public static EmulatorThread				EmulatorThreadObject			= null;
	public static SkinBase						CurrentSkin						= null;

	public static Vibrator						VibratorService					= null;
	public static ProgressDialogControl			ProgressDialogObj				= new ProgressDialogControl();

	public static Dimension2D					AndroidDeviceScreenDimension	= null;
	public static int							Orientation						= ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
	public static Date							LastTouched						= null;
	public static boolean						UseVolumeAsMenu					= false;
	public static String						UserEmail						= null;
	public static boolean						InitComplete					= false;

	public static volatile boolean				IsEmulating						= false;

	private static boolean						InFirstScreen					= false;

	public static volatile ArrayList<String>	UploadFilesPath					= null;
	public static volatile boolean				SyncClock						= false;
	
	public static int lastButtonPressed = -1;
	public static int lastlastButtonPressed = -1;
 
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		InitScreenFlags();
		setContentView(R.layout.emulator_main);
		BackwardCompatibility.RunPatches(this);
		InitMembers();
	/*	
		try
		{
			Thread.sleep(8 * 1000);
		}
		catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		IsEmulating = false;

		if (UIStateManagerObj == null || UIStateManagerObj.ControlBarIntstance == null)
		{
			InitMembers();
		}

		StartGraph89();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
	}

	@Override
	protected void onStop()
	{
		super.onStop();

		EngineExit();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		EngineExit();

		///Out of memory (java.lang.OutOfMemoryError: bitmap size exceeds VM budget)///
		try
		{
			unbindDrawables(findViewById(R.id.RootView));
			System.gc();
		}
		catch (Exception e)
		{
		}
	}

	private void unbindDrawables(View view)
	{
		if (view == null) return;

		if (view.getBackground() != null)
		{
			view.getBackground().setCallback(null);
		}
		if (view instanceof ViewGroup && !(view instanceof AdapterView))
		{
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
			{
				unbindDrawables(((ViewGroup) view).getChildAt(i));
			}
			((ViewGroup) view).removeAllViews();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (!InitComplete) return false;

		LastTouched = new Date();

		switch (keyCode)
		{
			case KeyEvent.KEYCODE_MENU:
			case KeyEvent.KEYCODE_BACK:
				if (UIStateManagerObj != null) UIStateManagerObj.BackKeyPressed();
				return true;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
			case KeyEvent.KEYCODE_VOLUME_UP:
				if (UseVolumeAsMenu()) return true;
				break;
		}

		if (IsEmulating)
		{
			if (ActiveInstance.CalculatorType == CalculatorTypes.TI89 || ActiveInstance.CalculatorType == CalculatorTypes.TI89T || ActiveInstance.CalculatorType == CalculatorTypes.UNKNOWN)
			{
				if (TI89Specific.ProcessKeyPress(keyCode, event)) return true;
			}
			else if (ActiveInstance.CalculatorType == CalculatorTypes.V200 || ActiveInstance.CalculatorType == CalculatorTypes.TI92PLUS || ActiveInstance.CalculatorType == CalculatorTypes.TI92)
			{
				if (V200Specific.ProcessKeyPress(keyCode, event)) return true;
			}
			else if (CalculatorTypes.isTilem(ActiveInstance.CalculatorType))
			{
				if (TI84Specific.ProcessKeyPress(keyCode, event)) return true;
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event)
	{
		if (!InitComplete) return false;
		if (keyCode == KeyEvent.KEYCODE_MENU)
		{
			ShowKeyboard();
			return true;
		}

		return super.onKeyLongPress(keyCode, event);
	}

	protected void StartGraph89()
	{
		CalculatorInstances = new CalculatorInstanceHelper(this);

		UIStateManagerObj.ControlBarIntstance.SetListOfCalculatorTypes(GetCalculatorInstances());

		ShowWhatsNew();

		InFirstScreen = false;
		if (CalculatorInstances.GetInstances().size() == 0)
		{
			InFirstScreen = true;
			try {
				ShowIntroText();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			KillCalc();

			GetActiveCalculatorInstance();

			CheckOrientation();
			StartCalc();
		}
	}

	private void StartCalc()
	{
		synchronized (EmulatorThread.EmulatorLock)
		{
			ButtonState.Reset();
			lastButtonPressed = -1;
			lastlastButtonPressed = -1;
			
			UIStateManagerObj.ShowCalc();
			InitSkin();
			
			//fix
			if (CurrentSkin == null) return;
			
			double speedCoefficient = EmulatorActivity.ActiveInstance.Configuration.CPUSpeed / 100.0f;
			
			EmulatorActivity.nativeInitGraph89(ActiveInstance.CalculatorType,
					CurrentSkin.Screen.mRawScreenWidth, CurrentSkin.Screen.mRawScreenHeight, CurrentSkin.Screen.Zoom, 
					Util.Bool2Int(ActiveInstance.Configuration.EnableGrayScale), Util.Bool2Int(ActiveInstance.Configuration.UseLCDGrid), 
					CurrentSkin.LCDPixelON, CurrentSkin.LCDPixelOFF, CurrentSkin.LCDGRID, speedCoefficient, Directories.getTempDirectory(this));
			
			if (CalculatorTypes.isTIEmu(ActiveInstance.CalculatorType))
			{
				EmulatorThreadObject = new TIEmuThread(this, ActiveInstance);
			}
			else if (CalculatorTypes.isTilem(ActiveInstance.CalculatorType))
			{
				EmulatorThreadObject = new TilEmThread(this, ActiveInstance);
			}
		}
	}

	private void KillCalc()
	{
		if (EmulatorThreadObject != null)
		{
			EmulatorThreadObject.Kill();
			EmulatorThreadObject = null;
		}
		
		EmulatorActivity.nativeCleanGraph89();
	}

	private void EngineExit()
	{
		KillCalc();
		EmulatorScreen.EngineScreenParams.Reset();

		if (ProgressDialogObj.Dialog != null) ProgressDialogObj.Dialog.dismiss();
		ProgressDialogObj.Dialog = null;
		AlertControlObj.DismissAlert();
		UIStateManagerObj = null;
	}

	public void Terminate()
	{
		EngineExit();
		this.finish();
	}

	private void CheckOrientation()
	{
		boolean newAndroid = Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO;

		int auto = newAndroid ? ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR : ActivityInfo.SCREEN_ORIENTATION_SENSOR;
		int portrait = newAndroid ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		int landscape = newAndroid ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

		int calcType = ActiveInstance.CalculatorType;

		if (calcType == CalculatorTypes.TI89 || calcType == CalculatorTypes.TI89T || CalculatorTypes.isTilem(calcType))
		{
			if (ActiveInstance.Configuration.Orientation.equals("Auto"))
			{
				this.setRequestedOrientation(auto);
			}
			else if (ActiveInstance.Configuration.Orientation.equals("Portrait"))
			{
				this.setRequestedOrientation(portrait);
			}
			else if (ActiveInstance.Configuration.Orientation.equals("Landscape"))
			{
				this.setRequestedOrientation(landscape);
			}
		}
		else if (calcType == CalculatorTypes.V200 || calcType == CalculatorTypes.TI92 || calcType == CalculatorTypes.TI92PLUS)
		{
			this.setRequestedOrientation(landscape);
		}
	}
	
	public boolean IsTilem2ndOffPressed()
	{
		return lastButtonPressed == 41 && lastlastButtonPressed == 54;
	}

	public void HideActions()
	{
		if (!InitComplete) return;
		UIStateManagerObj.HideActions();
	}

	public void RestartEmulator()
	{
		StartGraph89();
	}

	private void InitSkin()
	{
		if (CurrentSkin != null)
		{
			CurrentSkin.ReleaseBitmaps();
			CurrentSkin = null;
		}

		if (ActiveInstance.Configuration.Skin == SkinDefinition.BUILD_IN_UNKNOWN)
		{
			if (ActiveInstance.CalculatorType == CalculatorTypes.TI89)
			{
				ActiveInstance.Configuration.Skin = SkinDefinition.BUILD_IN_89_CLASSIC;
			}
			else if (ActiveInstance.CalculatorType == CalculatorTypes.TI89T)
			{
				ActiveInstance.Configuration.Skin = SkinDefinition.BUILD_IN_89T_CLASSIC;
			}
			else if (CalculatorTypes.isTilem(ActiveInstance.CalculatorType))
			{
				ActiveInstance.Configuration.Skin = SkinDefinition.BUILD_IN_TI84_CLASSIC;
			}
			else if (ActiveInstance.CalculatorType == CalculatorTypes.V200)
			{
				ActiveInstance.Configuration.Skin = SkinDefinition.BUILD_IN_LANDSCAPE_V200_CLASSIC;
			}
			else if (ActiveInstance.CalculatorType == CalculatorTypes.TI92 || ActiveInstance.CalculatorType == CalculatorTypes.TI92PLUS)
			{
				ActiveInstance.Configuration.Skin = SkinDefinition.BUILD_IN_LANDSCAPE_TI92P_CLASSIC;
			}
			else if (CalculatorTypes.isTilem(ActiveInstance.CalculatorType))
			{
				ActiveInstance.Configuration.Skin = SkinDefinition.BUILD_IN_TI84_CLASSIC;
			}
			else
			{
				ActiveInstance.Configuration.Skin = SkinDefinition.BUILD_IN_89_CLASSIC;
			}

			CalculatorInstances.Save();
		}

		SkinDefinition skinDefinition = new SkinDefinition(ActiveInstance.Configuration.Skin, IsPortrait());

		switch (ActiveInstance.CalculatorType)
		{
			case CalculatorTypes.UNKNOWN:
			case CalculatorTypes.TI89:
			case CalculatorTypes.TI89T:
				if (IsPortrait())
				{
					CalculatorInfoTI89 calculatorInfo89 = new CalculatorInfoTI89();
					CurrentSkin = new PortraitSkin(this, calculatorInfo89, skinDefinition);
				}
				else
				{
					CalculatorInfoTI89 calculatorInfo89 = new CalculatorInfoTI89();
					CurrentSkin = new LandscapeSkin(this, calculatorInfo89, skinDefinition);
				}
				break;
			case CalculatorTypes.TI92:
			case CalculatorTypes.TI92PLUS:
			case CalculatorTypes.V200:
				CalculatorInfoV200 calculatorInfoV200 = new CalculatorInfoV200();
				CurrentSkin = new LandscapeSkin(this, calculatorInfoV200, skinDefinition);
				break;
			case CalculatorTypes.TI83:
			case CalculatorTypes.TI83PLUS:
			case CalculatorTypes.TI83PLUS_SE:
			case CalculatorTypes.TI84PLUS:
			case CalculatorTypes.TI84PLUS_SE:
				if (IsPortrait())
				{
					CalculatorInfoTI84 calculaStorInfoTI84 = new CalculatorInfoTI84();
					CurrentSkin = new PortraitSkin(this, calculaStorInfoTI84, skinDefinition);
				}
				else
				{
					CalculatorInfoTI84 calculaStorInfoTI84 = new CalculatorInfoTI84();
					CurrentSkin = new LandscapeSkin(this, calculaStorInfoTI84, skinDefinition);
				}
				
				break;
		}

		try
		{
			CurrentSkin.Init(AndroidDeviceScreenDimension.Width, AndroidDeviceScreenDimension.Height);
		}
		catch (IOException e)
		{
			Util.ShowAlert(this, "InitSkin", e);
		}
	}

	public static void SendKeyToCalc(int key, int active, boolean haptic)
	{
		if (IsEmulating)
		{
			if (haptic)
			{
				LastTouched = new Date();

				if (VibratorService != null && ActiveInstance.Configuration.HapticFeedback > 0) VibratorService.vibrate(ActiveInstance.Configuration.HapticFeedback);

				if (ActiveInstance.Configuration.AudioFeedBack)
				{
					UIStateManagerObj.MessageViewIntstance.playSoundEffect(SoundEffectConstants.CLICK);
				}
			}
			
			if(active != 0)
			{
				EmulatorActivity.lastlastButtonPressed = EmulatorActivity.lastButtonPressed;
				EmulatorActivity.lastButtonPressed = key;
			}
			
			nativeSendKey(key, active);
		}
	}

	public static void SendKeysToCalc(int[] keys)
	{
		if (IsEmulating)
		{
			LastTouched = new Date();

			int[] keysonoff = new int[keys.length * 2];

			for (int i = 0; i < keys.length; ++i)
			{
				keysonoff[i * 2] = keys[i];
				keysonoff[i * 2 + 1] = keys[i] | 0x80;
			}

			nativeSendKeys(keysonoff);
		}
	}

	public void SetText(int priority, String text)
	{
		if (!InitComplete) return;

		UIStateManagerObj.MessageViewIntstance.SetText(priority, text);
		UIStateManagerObj.ShowTextViewer();
	}

	public void SetTextSpannable(int priority, Spannable text)
	{
		if (!InitComplete) return;

		UIStateManagerObj.MessageViewIntstance.SetSpannable(priority, text);
		UIStateManagerObj.ShowTextViewer();
	}

	public void ShowKeyboard()
	{
		if (!InitComplete) return;

		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);

		inputMethodManager.toggleSoftInputFromWindow(UIStateManagerObj.EmulatorViewIntstance.getWindowToken(), InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
	}

	public void HideKeyboard()
	{
		if (!InitComplete) return;
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	private void ShowIntroText() throws IOException
	{
		Resources r = getResources();
		float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, r.getDisplayMetrics());
		float py = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, r.getDisplayMetrics());

		Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.backbutton);
		b = Bitmap.createScaledBitmap(b, (int) px, (int) py, true);

		ImageSpan is = new ImageSpan(this, b, ImageSpan.ALIGN_BASELINE);

		StringBuilder msg = new StringBuilder();
		msg.append("Press the 'BACK' Button [ ] to show the Menu.\n\n");

		msg.append("\nGraph 89 emulates the following calculators:\nTI-83, TI-83 Plus, TI-83 Plus SE, TI-84 Plus, TI-84 Plus SE, TI-89, TI-89 Titanium, TI92, TI 92 Plus and Voyage 200" + "\n\n1. You should own the TI calculator you're willing to emulate. If you don't, STOP here. You cannot use an emulator if you do not own the hardware!!!\n\n" + "2. You need to extract the ROM image from your own calculator. ROM images can be installed by copying them to the internal memory of your Android device, pressing the BACK button and selecting 'ROM Manager'\n" + "Accepted file formats: *.rom, *.8Xu, *.89u, *.v2u, *.9xu, *.tib. Please read the Graph89 description on Google Play Store as it might contain aditional information\n\n" + "3. In order to avoid corruption, always transfer the ROM file to your Android device by using a USB cable. DO NOT use a browser or an email client.\n\nDetailed instructions:\nhttp://www.ticalc.org/programming/emulators/romdump.html\n\nSoftware such as TiLP II can be used to extract the ROM. http://sourceforge.net/projects/tilp/files/\n\n\n" + "For more information visit www.graph89.com\n\n\n\n");

		InputStream fstream = this.getAssets().open("license.txt");

		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));

		String strLine;

		while ((strLine = br.readLine()) != null)
		{
			msg.append(strLine).append("\n");
		}
		
		br.close();
		
		SpannableString ss = new SpannableString(msg.toString());
		ss.setSpan(is, 25, 26, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

		SetTextSpannable(0, ss); // lowest priority (0)
	}

	public void StartGallery()
	{
		if (ScreenshotTaker.LastFile == null) return;

		Intent intent = new Intent();
		intent.setAction(android.content.Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(ScreenshotTaker.LastFile), "image/png");
		startActivity(intent);
	}

	private void ShowWhatsNew()
	{
		SharedPreferences settings = getSharedPreferences("TI_EMU_DH", Context.MODE_PRIVATE);
		String shown = settings.getString("WhatsNew1.1.3.3Shown", null);
		if (shown == null)
		{
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("WhatsNew1.1.3.3Shown", "Shown");
			editor.commit();
			WhatsNew wn = new WhatsNew(this);
			wn.Show();
		}
	}

	private List<String> GetCalculatorInstances()
	{
		ArrayList<String> titleList = new ArrayList<String>();

		List<CalculatorInstance> instances = CalculatorInstances.GetInstances();
		for (int i = 0; i < instances.size(); ++i)
		{
			titleList.add(instances.get(i).Title);
		}

		return titleList;
	}

	private boolean UseVolumeAsMenu()
	{
		if (InFirstScreen)
		{
			AlertDialog alert = new AlertDialog.Builder(this).setTitle("Menu Access").setMessage("Use the Volume keys to access Menu?").setNegativeButton(android.R.string.no, new Dialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface d, int which)
				{
					SharedPreferences settings = getSharedPreferences("TI_EMU_DH", Context.MODE_PRIVATE);
					SharedPreferences.Editor editor = settings.edit();
					editor.remove("VOLUME_AS_MENU");
					UseVolumeAsMenu = false;
					editor.commit();
				}
			}).setPositiveButton(android.R.string.yes, new Dialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface d, int which)
				{
					SharedPreferences settings = getSharedPreferences("TI_EMU_DH", Context.MODE_PRIVATE);
					SharedPreferences.Editor editor = settings.edit();
					editor.putBoolean("VOLUME_AS_MENU", true);
					editor.commit();
					UseVolumeAsMenu = true;
				}
			}).create();
			alert.show();
		}

		if (UseVolumeAsMenu)
		{
			UIStateManagerObj.BackKeyPressed();
			return true;
		}
		else
		{
			return false;
		}
	}

	private boolean IsPortrait()
	{
		//fix
		return Orientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
	}

	private void InitScreenFlags()
	{
		android.view.Window w = this.getWindow();
		w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	private void InitMembers()
	{
		synchronized (EmulatorThread.EmulatorLock)
		{
			IsEmulating = false;
			UIStateManagerObj = new UIStateManager(this);
			GetDisplaySize();
			CalculatorInstances = new CalculatorInstanceHelper(this);
			GetActiveCalculatorInstance();
			UserEmail = GoogleAccount.getEmail(this);

			UIStateManagerObj.EmulatorViewIntstance.setOnTouchListener(UIStateManagerObj.EmulatorViewIntstance);
			VibratorService = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

			LastTouched = new Date();

			SharedPreferences settings = getSharedPreferences("TI_EMU_DH", Context.MODE_PRIVATE);
			UseVolumeAsMenu = settings.getBoolean("VOLUME_AS_MENU", false);

			lastButtonPressed = -1;
			lastlastButtonPressed = -1;
			InitComplete = true;
		}
	}

	private void GetActiveCalculatorInstance()
	{
		synchronized (EmulatorThread.EmulatorLock)
		{
			int id = CalculatorInstances.GetLastUsedInstanceID();

			if (id >= 0)
			{
				ActiveInstance = CalculatorInstances.GetByIndex(id);
				ActiveInstanceIndex = id;
				UIStateManagerObj.ControlBarIntstance.CalculatorTypeSpinnerInstance.setSelection(id);
			}
			else
			{
				ActiveInstance = null;
				ActiveInstanceIndex = 0;
			}
		}
	}

	private void GetDisplaySize()
	{
		DisplayMetrics metrics = new DisplayMetrics();
		Display display = getWindowManager().getDefaultDisplay();
		display.getMetrics(metrics);

		AndroidDeviceScreenDimension = new Dimension2D();

		AndroidDeviceScreenDimension.Height = metrics.heightPixels;
		AndroidDeviceScreenDimension.Width = metrics.widthPixels;
//fix
		if (AndroidDeviceScreenDimension.Height > AndroidDeviceScreenDimension.Width)
		{
			Orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
		}
		else
		{
			Orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK)
		{
			switch (requestCode)
			{
				case EmulatorActivity.INSTALL_APP:
					if (data.hasExtra(FilePickerActivity.EXTRA_FILE_PATH))
					{
						UploadFilesPath = data.getStringArrayListExtra(FilePickerActivity.EXTRA_FILE_PATH);
					}
					break;
			}
		}
	}

	public static void TiEmuSetScreenParams(EmulatorScreen screen)
	{
		synchronized (EmulatorScreen.ScreenChangeLock)
		{
			EmulatorScreen.EngineScreenParams.RawHeight = screen.mRawScreenHeight;
			EmulatorScreen.EngineScreenParams.RawWidth = screen.mRawScreenWidth;
			EmulatorScreen.EngineScreenParams.Zoom = screen.Zoom;
			
			nativeUpdateScreenZoom(screen.Zoom);
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void HandlerStartGallery()
	{
		mHandler.sendEmptyMessage(EmulatorActivity.HANDLER_STARTGALLERY);
	}

	public void HandlerReceiveFile()
	{
		mHandler.sendEmptyMessage(EmulatorActivity.HANDLER_RECEIVEFILE);
	}

	public void HandlerShowProgressDialog()
	{
		mHandler.sendEmptyMessage(EmulatorActivity.HANDLER_SHOWPROGRESSDIALOG);
	}

	public void HandlerUpdateProgressDialog()
	{
		mHandler.sendEmptyMessage(EmulatorActivity.HANDLER_UPDATEPROGRESSDIALOG);
	}

	public void HandlerHideProgressDialog()
	{
		mHandler.sendEmptyMessage(EmulatorActivity.HANDLER_HIDEPROGRESSDIALOG);
	}

	public void HandlerTerminate()
	{
		mHandler.sendEmptyMessage(EmulatorActivity.HANDLER_TERMINATE);
	}

	private void ReceiveFile()
	{
		ReceivedFileSaver s = new ReceivedFileSaver(this, Directories.getReceivedDirectory(this));
		s.ShowDialog();
	}

	private void ShowProgressDialog()
	{
		if (ProgressDialogObj.Dialog != null) ProgressDialogObj.Dialog.dismiss();

		ProgressDialogObj.Dialog = new ProgressDialog(this);
		ProgressDialogObj.Dialog.setMessage(ProgressDialogObj.Message);
		ProgressDialogObj.Dialog.setCancelable(false);
		ProgressDialogObj.Dialog.show();
	}

	private void UpdateProgressDialog()
	{
		if (ProgressDialogObj.Dialog == null) return;
		ProgressDialogObj.Dialog.setMessage(ProgressDialogObj.Message);
	}

	private void HideProgressDialog()
	{
		if (ProgressDialogObj.Dialog == null) return;
		ProgressDialogObj.Dialog.dismiss();
		ProgressDialogObj.Dialog = null;
		ProgressDialogObj.Message = "";
	}

	@Override
	protected void handleMessage(Message msg)
	{
		super.handleMessage(msg);

		switch (msg.what)
		{
			case EmulatorActivity.HANDLER_STARTGALLERY:
				StartGallery();
				break;
			case EmulatorActivity.HANDLER_RECEIVEFILE:
				ReceiveFile();
				break;
			case EmulatorActivity.HANDLER_SHOWPROGRESSDIALOG:
				ShowProgressDialog();
				break;
			case EmulatorActivity.HANDLER_UPDATEPROGRESSDIALOG:
				UpdateProgressDialog();
				break;
			case EmulatorActivity.HANDLER_HIDEPROGRESSDIALOG:
				HideProgressDialog();
				break;
			case EmulatorActivity.HANDLER_TERMINATE:
				Terminate();
				break;				
		}
	}

	// ///////////////////////////////////////////////////////////////////////////////////////////

	//-----common------------------------------------------
	public native static void nativeInitGraph89(int calc_type, int screen_width, int screen_height, int zoom, int is_grayscale, int is_grid, int pixel_on_color, int pixel_off_color, int grid_color, double speed_coefficient, String tmp_dir);
	public native static void nativeCleanGraph89();
	public native static int  nativeInstallROM(String rom_source, String rom_destination, int calc_type, int is_rom);
	public native static int  nativeReadEmulatedScreen(byte[] returnFlags);
	public native static void nativeGetEmulatedScreen(int[] screenBuffer);
	public native static void nativeSendKey(int key, int active);
	public native static void nativeSendKeys(int[] keys);
	public native static void nativeUpdateScreenZoom(int zoom);
	
	//-----tiemu------------------------------------------
	public native static void nativeTiEmuStep1LoadDefaultConfig();
	public native static int  nativeTiEmuStep2LoadImage(String path);
	public native static int  nativeTiEmuStep3Init();
	public native static int  nativeTiEmuStep4Reset();
	public native static int  nativeTiEmuLoadState(String filename);
	public native static int  nativeTiEmuRunEngine();
	public native static int  nativeTiEmuSaveState(String filename);
	public native static void nativeTiEmuSyncClock();
	public native static void nativeTiEmuPatch(String sr, String version);
	public native static void nativeTiEmuTurnScreenOn();
	public native static int  nativeTiEmuUploadFile(String filename);
	
	//-----tilem--------------------------------------------
	public native static int  nativeTilemLoadImage(String path); 
	public native static void nativeTilemReset();
	public native static void nativeTilemTurnScreenOn();
	public native static int  nativeTilemRunEngine();
	public native static int  nativeTilemLoadState(String filename);
	public native static int  nativeTilemSaveState(String rom_filename, String state_filename);
	public native static int  nativeTilemUploadFile(String filename);
	public native static void nativeTilemSyncClock();

	static
	{
		System.loadLibrary("glib-2.0");
		System.loadLibrary("ticables2-1.3.3");
		System.loadLibrary("ticonv-1.1.3");
		System.loadLibrary("tifiles2-1.1.5");
		System.loadLibrary("ticalcs2-1.1.7");
		System.loadLibrary("tiemu-3.03");
		System.loadLibrary("tilem-2.0");
		System.loadLibrary("wrapper");
	}
}