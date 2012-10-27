/*
 *  Camstream, an AMQP-based video streaming toolkit.
 *  Copyright (C) 2007-2009 LShift Ltd. <query@lshift.net>
 *  Copyright (C) 2010-2012 Tony Garnock-Jones <tonygarnockjones@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>

#include <sys/ioctl.h>

#include <linux/videodev.h>

#include "jv4l.h"

static jfieldID lookup(JNIEnv *env, jobject obj, char *field, char *type) {
  jclass class = (*env)->GetObjectClass(env, obj);
  jfieldID fieldID = (*env)->GetFieldID(env, class, field, type);
  (*env)->DeleteLocalRef(env, class);
  if (fieldID == NULL) {
    fprintf(stderr, "Lookup of field %s (type %s) failed\n", field, type);
    return NULL;
  }
  return fieldID;
}

#define GETTER_SETTER(fieldctype, namesplicepart, gettersplicepart, typecode) \
  static fieldctype get ## namesplicepart ## Field(JNIEnv *env, jobject obj, char *field) { \
    return (*env)->Get ## gettersplicepart ## Field(env, obj, lookup(env, obj, field, typecode)); \
  } \
  static void set ## namesplicepart ## Field(JNIEnv *env, jobject obj, char *field, fieldctype val) { \
    (*env)->Set ## gettersplicepart ## Field(env, obj, lookup(env, obj, field, typecode), val); \
  }

GETTER_SETTER(jint, Int, Int, "I");
GETTER_SETTER(jstring, String, Object, "Ljava/lang/String;");

JNIEXPORT jint JNICALL Java_net_lshift_camcapture_v4l_Driver_open
  (JNIEnv *env, jobject self, jint deviceNumber)
{
  char devname[128];
  int fd;
  struct video_capability cap;

  if (snprintf(devname, sizeof(devname), "/dev/video%d", deviceNumber) >= sizeof(devname)) {
    // Buffer overflow.
    return -1;
  }

  printf("Opening '%s'\n", devname);
  fd = open(devname, O_RDWR, 0);
  if (fd == -1) {
    perror("jv4l open");
    return -1;
  }

  if (ioctl(fd, VIDIOCGCAP, &cap) == -1) {
    perror("jv4l ioctl VIDIOCGCAP");
    return -1;
  }

  setStringField(env, self, "deviceName", (*env)->NewStringUTF(env, cap.name));
  setIntField(env, self, "deviceType", cap.type);
  setIntField(env, self, "minWidth", cap.minwidth);
  setIntField(env, self, "minHeight", cap.minheight);
  setIntField(env, self, "maxWidth", cap.maxwidth);
  setIntField(env, self, "maxHeight", cap.maxheight);

  return fd;
}

JNIEXPORT jboolean JNICALL Java_net_lshift_camcapture_v4l_Driver_close
  (JNIEnv *env, jobject self, jint fd)
{
  if (close(fd) == -1) {
    perror("jv4l close");
    return JNI_FALSE;
  };

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_net_lshift_camcapture_v4l_Driver_getVideoPicture
  (JNIEnv *env, jobject self, jint fd, jobject vp)
{
  struct video_picture pict;

  if (ioctl(fd, VIDIOCGPICT, &pict) == -1) {
    perror("jv4l ioctl VIDIOCGPICT");
    return JNI_FALSE;
  }

  setIntField(env, vp, "brightness", pict.brightness);
  setIntField(env, vp, "hue", pict.hue);
  setIntField(env, vp, "colour", pict.colour);
  setIntField(env, vp, "contrast", pict.contrast);
  setIntField(env, vp, "whiteness", pict.whiteness);
  setIntField(env, vp, "depth", pict.depth);
  setIntField(env, vp, "palette", pict.palette);
  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_net_lshift_camcapture_v4l_Driver_setVideoPicture
  (JNIEnv *env, jobject self, jint fd, jobject vp)
{
  struct video_picture pict;

  pict.brightness = getIntField(env, vp, "brightness");
  pict.hue = getIntField(env, vp, "hue");
  pict.colour = getIntField(env, vp, "colour");
  pict.contrast = getIntField(env, vp, "contrast");
  pict.whiteness = getIntField(env, vp, "whiteness");
  pict.depth = getIntField(env, vp, "depth");
  pict.palette = getIntField(env, vp, "palette");

  if (ioctl(fd, VIDIOCSPICT, &pict) == -1) {
    perror("jv4l ioctl VIDIOCSPICT");
    return JNI_FALSE;
  } else {
    return JNI_TRUE;
  }
}

JNIEXPORT jboolean JNICALL Java_net_lshift_camcapture_v4l_Driver_getVideoWindow
  (JNIEnv *env, jobject self, jint fd, jobject vw)
{
  struct video_window win;

  if (ioctl(fd, VIDIOCGWIN, &win) == -1) {
    perror("jv4l ioctl VIDIOCGWIN");
    return JNI_FALSE;
  }

  setIntField(env, vw, "x", win.x);
  setIntField(env, vw, "y", win.y);
  setIntField(env, vw, "width", win.width);
  setIntField(env, vw, "height", win.height);
  setIntField(env, vw, "chromakey", win.chromakey);
  setIntField(env, vw, "flags", win.flags);
  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_net_lshift_camcapture_v4l_Driver_setVideoWindow
  (JNIEnv *env, jobject self, jint fd, jobject vw)
{
  struct video_window win;

  win.x = getIntField(env, vw, "x");
  win.y = getIntField(env, vw, "y");
  win.width = getIntField(env, vw, "width");
  win.height = getIntField(env, vw, "height");
  win.chromakey = getIntField(env, vw, "chromakey");
  win.flags = getIntField(env, vw, "flags");
  win.clips = NULL;
  win.clipcount = 0;

  if (ioctl(fd, VIDIOCSWIN, &win) == -1) {
    perror("jv4l ioctl VIDIOCSWIN");
    return JNI_FALSE;
  } else {
    return JNI_TRUE;
  }
}

JNIEXPORT jboolean JNICALL Java_net_lshift_camcapture_v4l_Driver_readFrame
  (JNIEnv *env, jobject self, jint fd, jbyteArray buffer)
{
  jbyte *bufptr = (*env)->GetByteArrayElements(env, buffer, NULL);
  size_t buflen = (*env)->GetArrayLength(env, buffer);
  ssize_t count = read(fd, bufptr, buflen);
  (*env)->ReleaseByteArrayElements(env, buffer, bufptr, 0);
  return (count == buflen) ? JNI_TRUE : JNI_FALSE;
}
