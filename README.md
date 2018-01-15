# DocumentScan
仿有道云笔记App端 文档扫描 功能

# Usage:
1、Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
2、Add it in your root build.gradle at the end of repositories:

			dependencies {
					compile 'com.github.pdliugithub:DocumentScan:v1.0.1'
			}

3、Code:
	//instance.
	CameraApiFragment cameraApiFragment = CameraApiFragment.newInstance();

	//add.
	getSupportFragmentManager().beginTransaction().add(R.id.container, cameraApiFragment, "api").commit();

	//take picture callback.
	cameraApiFragment.setTakePictureCallback(new TakePictureCallback() {
			@Override
			public void call(Bitmap bitmap) {
				showImg.setImageBitmap(bitmap);
			}
		});

[![](https://jitpack.io/v/pdliugithub/DocumentScan.svg)](https://jitpack.io/#pdliugithub/DocumentScan)
