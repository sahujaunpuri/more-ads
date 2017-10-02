/**
 * Copyright 2017 FrogSquare. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package org.godotengine.godot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.android.gms.ads.reward.RewardItem;

import com.godot.game.BuildConfig;
import com.godot.game.R;

import org.godotengine.godot.Godot;
import org.godotengine.godot.Utils;

import org.json.JSONObject;
import org.json.JSONException;

import org.godotengine.godot.Dictionary;

public class AdMob extends Godot.SingletonBase {

	static public Godot.SingletonBase initialize (Activity p_activity) {
		return new AdMob(p_activity);
	}

	public AdMob(Activity p_activity) {
		activity = p_activity;

		registerClass ("AdMob", new String[] {
		"init", "show_banner_ad", "show_interstitial_ad", "show_rewarded_video"
		});
	}

	public void init (final Dictionary p_dict, final int p_script_id) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				_config = new JSONObject(p_dict);
				_init();

				Utils.setScriptInstance(p_script_id);
				Utils.d("AdMob::Initialized");
			}
		});
	}

	private void _init() {
		if (_config.optBoolean("BannerAd", false)) {
			createBanner();
		}

		if (_config.optBoolean("InterstitialAd", false)) {
			createInterstitial();
		}

		if (_config.optBoolean("RewardedVideoAd", false)) {
			createRewardedVideo();
		}
	}

	public void createBanner() {
		FrameLayout layout = ((Godot)activity).layout; // Getting Godots framelayout
		FrameLayout.LayoutParams AdParams = new FrameLayout.LayoutParams(
						 FrameLayout.LayoutParams.MATCH_PARENT,
						 FrameLayout.LayoutParams.WRAP_CONTENT);

		if(mAdView != null) { layout.removeView(mAdView); }

		if (_config.optString("BannerGravity", "BOTTOM").equals("BOTTOM")) {
			AdParams.gravity = Gravity.BOTTOM;
		} else { AdParams.gravity = Gravity.TOP; }

		AdRequest.Builder adRequestB = new AdRequest.Builder();
		adRequestB.tagForChildDirectedTreatment(true);

		// Show test ads if the build flaged debug
		if (BuildConfig.DEBUG) {
			adRequestB.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
			adRequestB.addTestDevice(Utils.getDeviceId(activity));
		}

		AdRequest adRequest = adRequestB.build();

		String ad_unit_id = _config.optString("BannerAdId", "");

		if (ad_unit_id.length() <= 0) {
			Utils.d("AdMob:Banner:UnitId:NotProvided");
			ad_unit_id = activity.getString(R.string.banner_ad_unit_id);
		}

		mAdView = new AdView(activity);
		mAdView.setBackgroundColor(Color.TRANSPARENT);
		mAdView.setAdUnitId(ad_unit_id);
		mAdView.setAdSize(AdSize.SMART_BANNER);

		mAdView.setAdListener(new AdListener() {
			@Override
			public void onAdLoaded() {
				Utils.d("AdMob:Banner:OnAdLoaded");
				Utils.callScriptFunc("AdMob", "AdMob_Banner", "loaded");
			}

			@Override
			public void onAdFailedToLoad(int errorCode) {
				Utils.w("AdMob:Banner:onAdFailedToLoad:" + errorCode);
				Utils.callScriptFunc("AdMob", "AdMob_Banner", "load_failed");
			}
		});

		mAdView.setVisibility(View.INVISIBLE);
		mAdView.loadAd(adRequest);

		layout.addView(mAdView, AdParams);
	}

	public void createInterstitial() {
		String ad_unit_id = _config.optString("InterstitialAdId", "");

		if (ad_unit_id.length() <= 0) {
			Utils.d("AdMob:Interstitial:UnitId:NotProvided");
			ad_unit_id = activity.getString(R.string.interstitial_ad_unit_id);
		}

		mInterstitialAd = new InterstitialAd(activity);
		mInterstitialAd.setAdUnitId(ad_unit_id);
		mInterstitialAd.setAdListener(new AdListener() {
			@Override
			public void onAdLoaded() {
				Utils.d("AdMob:Interstitial:OnAdLoaded");
				Utils.callScriptFunc("AdMob", "AdMob_Interstitial", "loaded");
			}

			@Override
			public void onAdFailedToLoad(int errorCode) {
				Utils.w("AdMob:Interstitial:onAdFailedToLoad:" + errorCode);
				Utils.callScriptFunc("AdMob", "AdMob_Interstitial", "load_failed");
			}

			@Override
			public void onAdClosed() {
				Utils.w("AdMob:Interstitial:onAdClosed");
				requestNewInterstitial();
			}

		});

		requestNewInterstitial();
	}

	private void requestNewInterstitial() {
		AdRequest.Builder adRB = new AdRequest.Builder();

		if (BuildConfig.DEBUG) {
			adRB.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
			adRB.addTestDevice(Utils.getDeviceId(activity));
		}

		AdRequest adRequest = adRB.build();

		mInterstitialAd.loadAd(adRequest);
	}

	public void createRewardedVideo() {
		mrv = MobileAds.getRewardedVideoAdInstance(activity);
		mrv.setRewardedVideoAdListener(new RewardedVideoAdListener() {

			@Override
			public void onRewardedVideoAdLoaded() {
				Utils.d("AdMob:Video:Loaded");
				//emitRewardedVideoStatus();
				//Utils.call
			}

			@Override
			public void onRewarded(RewardItem rewardItem) {
				Utils.d("AdMob:Rewarded");

				JSONObject ret = new JSONObject();
				try {
					ret.put("RewardType", rewardItem.getType());
					ret.put("RewardAmount", rewardItem.getAmount());
				} catch (JSONException e) {
					Utils.d("AdMob:Reward:Error:" + e.toString());
				}

				Utils.callScriptFunc("AdMob", "AdMobReward", ret.toString());
			}

			@Override
			public void onRewardedVideoAdFailedToLoad(int errorCode) {
				Utils.d("AdMob:VideoLoad:Failed");
				Utils.callScriptFunc("AdMob", "AdMob_Video", "load_failed");
			}

			@Override
			public void onRewardedVideoAdClosed() {
				Utils.d("AdMob:VideoAd:Closed");
			}

			@Override
			public void onRewardedVideoAdLeftApplication() {
				Utils.d("AdMob:VideoAd:LeftApp");
			}

			@Override
			public void onRewardedVideoAdOpened() {
				Utils.d("AdMon:VideoAd:Opended");
			}

			@Override
			public void onRewardedVideoStarted() {
				Utils.d("Reward:VideoAd:Started");
			}
		});

		requestNewRewardedVideo();
	}

	private void requestNewRewardedVideo() {
		if (_config == null) { return; }

		AdRequest.Builder adRB = new AdRequest.Builder();

		if (BuildConfig.DEBUG) {
			adRB.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
			adRB.addTestDevice(Utils.getDeviceId(activity));
		}

		String ad_unit_id = _config.optString("RewardedVideoAdId", "");

		if (ad_unit_id.length() <= 0) {
			Utils.d("AdMob:RewardedVideo:UnitId:NotProvided");
			ad_unit_id = activity.getString(R.string.rewarded_video_ad_unit_id);
		}

		mrv.loadAd(ad_unit_id, adRB.build());
	}

	public void show_banner_ad(final boolean show) {
		if (mAdView == null) { return; }

		activity.runOnUiThread(new Runnable() {
			public void run() {
				if (show) {
					if (mAdView.isEnabled()) { mAdView.setEnabled(true); }
					if (mAdView.getVisibility() == View.INVISIBLE) {
						Utils.d("AdMob:Visiblity:On");
						mAdView.setVisibility(View.VISIBLE);
				}
				} else {
					if (mAdView.isEnabled()) { mAdView.setEnabled(false); }
					if (mAdView.getVisibility() != View.INVISIBLE) {
						Utils.d("AdMob:Visiblity:Off");
						mAdView.setVisibility(View.INVISIBLE);
					}
				}
			}
		});
	}

	public void show_interstitial_ad() {
		if (mInterstitialAd == null) { return; }

		activity.runOnUiThread(new Runnable() {
			public void run() {
				if (mInterstitialAd.isLoaded()) { mInterstitialAd.show(); }
				else { Utils.d("AdMob:Interstitial:NotLoaded"); }
			}
		});
	}

	public void show_rewarded_video() {
		if (mrv == null) { return; }

		activity.runOnUiThread(new Runnable() {
			public void run() {
				if (mrv.isLoaded()) { mrv.show(); }
				else { Utils.d("AdMob:RewardedVideo:NotLoaded"); }
			}
		});
	}

	public void onStart() {

	}

	public void onPause() {
		if (mAdView != null) { mAdView.pause(); }
		if (mrv != null) { mrv.pause(activity); }
	}

	public void onResume() {
		if (mAdView != null) { mAdView.resume(); }
		if (mrv != null) { mrv.resume(activity); }
	}

	public void onStop() {
		if (mAdView != null) { mAdView.destroy(); }
		if (mrv != null) { mrv.destroy(activity); }
	}

	private static Activity activity = null;
	private static AdMob mInstance = null;

	private AdView mAdView = null;
	private RewardedVideoAd mrv = null;
	private InterstitialAd mInterstitialAd = null;

	private JSONObject _config = null;

	private int _script_id = -1;
}

