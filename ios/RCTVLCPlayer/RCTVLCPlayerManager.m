#import "RCTVLCPlayerManager.h"
#import "RCTVLCPlayer.h"
#import "React/RCTBridge.h"

@implementation RCTVLCPlayerManager

RCT_EXPORT_MODULE();

@synthesize bridge = _bridge;

- (UIView *)view
{
    return [[RCTVLCPlayer alloc] initWithEventDispatcher:self.bridge.eventDispatcher];
}


- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

RCT_EXPORT_VIEW_PROPERTY(source, NSDictionary);
RCT_EXPORT_VIEW_PROPERTY(paused, BOOL);
RCT_EXPORT_VIEW_PROPERTY(seek, float);
RCT_EXPORT_VIEW_PROPERTY(rate, float);
RCT_EXPORT_VIEW_PROPERTY(muted, BOOL);
RCT_EXPORT_VIEW_PROPERTY(volume, int);
RCT_EXPORT_VIEW_PROPERTY(volumeUp, int);
RCT_EXPORT_VIEW_PROPERTY(volumeDown, int);
RCT_EXPORT_VIEW_PROPERTY(resume, BOOL);
RCT_EXPORT_VIEW_PROPERTY(clear, BOOL);
RCT_EXPORT_VIEW_PROPERTY(seekTime, int);
RCT_EXPORT_VIEW_PROPERTY(videoAspectRatio, NSString);
RCT_EXPORT_VIEW_PROPERTY(snapshotPath, NSString);
/* Should support: onLoadStart, onLoad, and onError to stay consistent with Image */
/*RCT_EXPORT_VIEW_PROPERTY(onVideoPaused, RCTDirectEventBlock);
 RCT_EXPORT_VIEW_PROPERTY(onVideoStopped, RCTDirectEventBlock);
 RCT_EXPORT_VIEW_PROPERTY(onVideoBuffering, RCTDirectEventBlock);
 RCT_EXPORT_VIEW_PROPERTY(onVideoPlaying, RCTDirectEventBlock);
 RCT_EXPORT_VIEW_PROPERTY(onVideoEnded, RCTDirectEventBlock);
 RCT_EXPORT_VIEW_PROPERTY(onVideoError, RCTDirectEventBlock);
 RCT_EXPORT_VIEW_PROPERTY(onVideoOpen, RCTDirectEventBlock);
 */
RCT_EXPORT_VIEW_PROPERTY(onVideoLoadStart, RCTDirectEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onVideoProgress, RCTDirectEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onSnapshot, RCTDirectEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onIsPlaying, RCTDirectEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onVideoStateChange, RCTDirectEventBlock);


+ (BOOL)requiresMainQueueSetup
{
    return YES;
}

@end
