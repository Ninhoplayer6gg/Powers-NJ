"""
Pure Python forward kinematics of a suit rig (.bbmodel authoring rig, Blockbench semantics).

Used while authoring clips: where do the feet/fists end up for a pose, how far below the ground is
the lowest foot... Matrices are 4x4 nested lists in Blockbench internal coordinates (pixels, Y up,
the character faces -Z, its right side is +X).
"""
import math

from . import bbmodel


def _mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def _translate(x, y, z):
    return [[1, 0, 0, x], [0, 1, 0, y], [0, 0, 1, z], [0, 0, 0, 1]]


def _rx(a):
    c, s = math.cos(a), math.sin(a)
    return [[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]]


def _ry(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]]


def _rz(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]


def apply(m, p):
    return [m[i][0] * p[0] + m[i][1] * p[1] + m[i][2] * p[2] + m[i][3] for i in range(3)]


class Rig:
    def __init__(self, model):
        self.model = model
        self.elements = bbmodel.elements_by_uuid(model)
        self.groups = bbmodel.groups_by_name(model)
        self.parent = {g['name']: (p['name'] if p else None) for g, p in bbmodel.iter_groups(model['outliner'])}

    def matrices(self, pose):
        """bone -> world matrix for a pose {bone: {'rot': [...], 'pos': [...]}} (Bedrock values)."""
        result = {}

        def walk(group, parent_matrix):
            origin = group.get('origin', [0, 0, 0])
            rest = group.get('rotation', [0, 0, 0])
            entry = pose.get(group['name'], {})
            rot = entry.get('rot', [0, 0, 0])
            pos = entry.get('pos', [0, 0, 0])
            rx = math.radians(rest[0] - rot[0])
            ry = math.radians(rest[1] - rot[1])
            rz = math.radians(rest[2] + rot[2])
            local = _translate(-pos[0], pos[1], pos[2])
            local = _mul(local, _translate(*origin))
            local = _mul(local, _mul(_rz(rz), _mul(_ry(ry), _rx(rx))))
            local = _mul(local, _translate(-origin[0], -origin[1], -origin[2]))
            matrix = _mul(parent_matrix, local)
            result[group['name']] = matrix
            for child in bbmodel.child_groups(group):
                walk(child, matrix)

        identity = [[1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]
        for top in self.model['outliner']:
            if isinstance(top, dict):
                walk(top, identity)
        return result

    def corners(self, bone, pose, matrices=None, recursive=False):
        """World corners of the cubes of a bone (and of its descendants when recursive)."""
        matrices = matrices or self.matrices(pose)
        points = []
        names = [bone]
        if recursive:
            names = [n for n in self.groups if self._descends(n, bone)]
        for name in names:
            group = self.groups[name]
            for element in bbmodel.direct_elements(group, self.elements):
                lo = [min(a, b) for a, b in zip(element['from'], element['to'])]
                hi = [max(a, b) for a, b in zip(element['from'], element['to'])]
                for x in (lo[0], hi[0]):
                    for y in (lo[1], hi[1]):
                        for z in (lo[2], hi[2]):
                            points.append(apply(matrices[name], (x, y, z)))
        return points

    def _descends(self, name, ancestor):
        while name is not None:
            if name == ancestor:
                return True
            name = self.parent.get(name)
        return False

    def point(self, bone, local, pose, matrices=None):
        matrices = matrices or self.matrices(pose)
        return apply(matrices[bone], local)

    def lowest(self, bones, pose):
        matrices = self.matrices(pose)
        return min(p[1] for b in bones for p in self.corners(b, pose, matrices))

    def centre(self, bones, pose):
        matrices = self.matrices(pose)
        pts = [p for b in bones for p in self.corners(b, pose, matrices)]
        return [sum(p[i] for p in pts) / len(pts) for i in range(3)]
