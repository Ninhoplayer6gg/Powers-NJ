"""
Animation clips: keyframes, sampling and conversion between .bbmodel animations and Bedrock
animation JSON (the format GeckoLib, Blockbench and the Powers NJ runtime read).

Value conventions are Bedrock/Blockbench ones (degrees, pixels). For the humanoid parts they equal
Minecraft's ModelPart rotations: negative X raises an arm or a leg forward, positive X on the head
looks down, positive Z on the right arm lifts it sideways.

Interpolation semantics (identical in com.powersnj.core.animation.Channel):
* the segment between two keyframes uses the mode of the destination keyframe;
* linear interpolates from the previous "post" value to the destination "pre" value;
* catmullrom uses uniform Catmull-Rom with the neighbouring keyframes (clamped at the ends);
* "pre"/"post" pairs create jumps (a "pre" equal to the previous value gives a step);
* before the first keyframe the first "pre" holds, after the last one the last "post" holds.
"""
from . import bbmodel

CHANNELS = ('rotation', 'position', 'scale')
LOOP_MODES = ('loop', 'once', 'hold')


class Key:
    __slots__ = ('time', 'pre', 'post', 'mode')

    def __init__(self, time, post, pre=None, mode='linear'):
        self.time = float(time)
        self.post = [float(v) for v in post]
        self.pre = [float(v) for v in (pre if pre is not None else post)]
        if mode not in ('linear', 'catmullrom', 'step'):
            raise ValueError('Unknown interpolation ' + str(mode))
        self.mode = mode


class Clip:
    def __init__(self, name, length, loop='once'):
        if loop not in LOOP_MODES:
            raise ValueError('loop must be one of ' + ', '.join(LOOP_MODES))
        self.name = name
        self.length = float(length)
        self.loop = loop
        self.bones = {}

    def channel(self, bone, channel):
        if channel not in CHANNELS:
            raise ValueError('Unknown channel ' + channel)
        return self.bones.setdefault(bone, {}).setdefault(channel, [])

    def add(self, bone, channel, key):
        keys = self.channel(bone, channel)
        keys.append(key)
        keys.sort(key=lambda k: k.time)
        return self

    def sample(self, bone, channel, time):
        keys = self.bones.get(bone, {}).get(channel)
        if not keys:
            return None
        return sample(keys, time)

    def local_time(self, elapsed):
        if self.loop == 'loop' and self.length > 0:
            return elapsed % self.length
        return min(elapsed, self.length)

    def pose(self, time):
        """bone -> {"rotation": [...], "position": [...]} at clip time (already wrapped)."""
        result = {}
        for bone, channels in self.bones.items():
            entry = {}
            for channel, keys in channels.items():
                entry[channel] = sample(keys, time)
            result[bone] = entry
        return result


def _catmull(p0, p1, p2, p3, t):
    t2 = t * t
    t3 = t2 * t
    return 0.5 * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3)


def sample(keys, time):
    if time <= keys[0].time:
        return list(keys[0].pre)
    if time >= keys[-1].time:
        return list(keys[-1].post)
    index = 1
    while keys[index].time <= time:
        index += 1
    a = keys[index - 1]
    b = keys[index]
    span = b.time - a.time
    f = 0.0 if span <= 0 else (time - a.time) / span
    if b.mode == 'step':
        return list(a.post)
    if b.mode == 'catmullrom':
        p0 = keys[index - 2].post if index >= 2 else a.post
        p3 = keys[index + 1].pre if index + 1 < len(keys) else b.pre
        return [_catmull(p0[i], a.post[i], b.pre[i], p3[i], f) for i in range(3)]
    return [a.post[i] + (b.pre[i] - a.post[i]) * f for i in range(3)]


# ---------------------------------------------------------------------------------- Bedrock JSON

def _num(v):
    v = round(float(v), 4)
    return 0 if v == 0 else (int(v) if v == int(v) else v)


def _time(t):
    text = ('%.4f' % t).rstrip('0')
    return text + '0' if text.endswith('.') else text


def to_bedrock(clips):
    animations = {}
    for clip in clips:
        bones = {}
        for bone, channels in clip.bones.items():
            out = {}
            for channel, keys in channels.items():
                frames = {}
                previous = None
                for key in keys:
                    post = [_num(v) for v in key.post]
                    pre = [_num(v) for v in key.pre]
                    if key.mode == 'step' and previous is not None:
                        pre = [_num(v) for v in previous.post]
                    if key.mode == 'catmullrom':
                        entry = {'post': post, 'lerp_mode': 'catmullrom'}
                        if pre != post:
                            entry['pre'] = pre
                    elif pre != post:
                        entry = {'pre': pre, 'post': post}
                    else:
                        entry = post
                    frames[_time(key.time)] = entry
                    previous = key
                out[channel] = frames
            bones[bone] = out
        animation = {'loop': {'loop': True, 'once': False, 'hold': 'hold_on_last_frame'}[clip.loop],
                     'animation_length': _num(clip.length), 'bones': bones}
        animations[clip.name] = animation
    return {'format_version': '1.8.0', 'animations': animations}


def _vector(value):
    if isinstance(value, (int, float)):
        return [float(value)] * 3
    if isinstance(value, str):
        return [float(value)] * 3
    return [float(v) for v in value]


def from_bedrock(data):
    clips = []
    for name, animation in data.get('animations', {}).items():
        loop = animation.get('loop', False)
        mode = 'loop' if loop is True else ('hold' if loop == 'hold_on_last_frame' else 'once')
        clip = Clip(name, animation.get('animation_length', 0), mode)
        for bone, channels in animation.get('bones', {}).items():
            for channel, frames in channels.items():
                if channel not in CHANNELS:
                    continue
                if not isinstance(frames, dict):
                    frames = {'0.0': frames}
                for time, entry in frames.items():
                    if isinstance(entry, dict):
                        post = _vector(entry.get('post', entry.get('pre')))
                        pre = _vector(entry.get('pre', entry.get('post')))
                        lerp = entry.get('lerp_mode', 'linear')
                        clip.add(bone, channel, Key(time, post, pre, 'catmullrom' if lerp == 'catmullrom' else 'linear'))
                    else:
                        clip.add(bone, channel, Key(time, _vector(entry)))
        if clip.length <= 0:
            clip.length = max([k.time for ch in clip.bones.values() for keys in ch.values() for k in keys] or [0])
        clips.append(clip)
    return clips


# ---------------------------------------------------------------------------------- .bbmodel

def _point(values):
    return {'x': _num(values[0]), 'y': _num(values[1]), 'z': _num(values[2])}


def write_bbmodel_animations(model, clips):
    """Replaces the animations of a .bbmodel (keyed by the rig group uuids)."""
    groups = bbmodel.groups_by_name(model)
    animations = []
    for clip in clips:
        animators = {}
        for bone, channels in clip.bones.items():
            if bone not in groups:
                raise ValueError('Clip %s animates unknown bone %s' % (clip.name, bone))
            keyframes = []
            for channel, keys in channels.items():
                for key in keys:
                    points = [_point(key.pre), _point(key.post)] if key.pre != key.post else [_point(key.post)]
                    keyframes.append({
                        'channel': channel,
                        'data_points': points,
                        'uuid': bbmodel.stable_uuid('kf', clip.name, bone, channel, key.time),
                        'time': round(key.time, 4),
                        'color': -1,
                        'interpolation': key.mode,
                    })
            animators[groups[bone]['uuid']] = {'name': bone, 'type': 'bone', 'keyframes': keyframes}
        animations.append({
            'uuid': bbmodel.stable_uuid('animation', clip.name),
            'name': clip.name,
            'loop': clip.loop,
            'override': False,
            'length': round(clip.length, 4),
            'snapping': 20,
            'selected': False,
            'anim_time_update': '',
            'blend_weight': '',
            'start_delay': '',
            'loop_delay': '',
            'animators': animators,
        })
    model['animations'] = animations


def read_bbmodel_animations(model, warnings=None):
    """Clips of a .bbmodel. Bezier keyframes are exported as catmullrom (Bedrock has no bezier)."""
    clips = []
    for animation in model.get('animations', []):
        clip = Clip(animation['name'], animation.get('length', 0), animation.get('loop', 'once'))
        for uuid, animator in animation.get('animators', {}).items():
            if animator.get('type', 'bone') != 'bone':
                continue
            bone = animator.get('name')
            for kf in animator.get('keyframes', []):
                channel = kf.get('channel')
                if channel not in CHANNELS:
                    continue
                points = kf.get('data_points', [])
                if not points:
                    continue
                values = [[float(p.get(axis, 0) or 0) for axis in ('x', 'y', 'z')] for p in points]
                mode = kf.get('interpolation', 'linear')
                if mode == 'bezier':
                    if warnings is not None:
                        warnings.append('%s/%s: bezier keyframe exported as catmullrom' % (clip.name, bone))
                    mode = 'catmullrom'
                if mode not in ('linear', 'catmullrom', 'step'):
                    mode = 'linear'
                clip.add(bone, channel, Key(kf['time'], values[-1], values[0], mode))
        clips.append(clip)
    return clips
