import {Address6} from 'ip-address';

type IceCandidateData = {
  candidate: string;
  messageType?: string;
  sdpMLineIndex?: string;
  sdpMid?: string;
  [key: string]: any;
};

const CANDIDATE_IP_PATTERN = /^[0-9a-f:.]+$/i;
const DEFAULT_CANDIDATE_PORT = 9002;
const XBOX_GAME_PORT = 3074;
const RFC1918_LAN_IPV4_PATTERN =
  /^(192\.168\.\d{1,3}\.\d{1,3}|10\.\d{1,3}\.\d{1,3}\.\d{1,3}|172\.(1[6-9]|2\d|3[0-1])\.\d{1,3}\.\d{1,3})$/;

function isPrivateLanIpv4Candidate(candidateStr: string): boolean {
  const tokens = candidateStr.trim().split(/\s+/);
  const ip = tokens[4] ?? '';
  return RFC1918_LAN_IPV4_PATTERN.test(ip);
}

export function expandTeredoCandidates(
  exchangeCandidates: IceCandidateData[],
  prioritizeIpv6 = false,
): IceCandidateData[] {
  const candidates = [...exchangeCandidates];
  let insertedCandidates = 0;

  exchangeCandidates.forEach((sourceCandidate, sourceIndex) => {
    const tokens = sourceCandidate.candidate.trim().split(/\s+/);
    const generated: IceCandidateData[] = [];

    for (let tokenIndex = 0; tokenIndex < tokens.length; tokenIndex++) {
      const address = tokens[tokenIndex];
      if (!CANDIDATE_IP_PATTERN.test(address) || !Address6.isValid(address)) {
        continue;
      }

      const teredo = new Address6(address).inspectTeredo();
      if (teredo.prefix !== '2001:0000') {
        continue;
      }
      const port = Number.parseInt(tokens[tokenIndex + 1], 10);
      const candidatePort = Number.isNaN(port) ? DEFAULT_CANDIDATE_PORT : port;
      const foundationStart = sourceIndex + 2 + generated.length;
      const addHostCandidate = (ip: string, hostPort: string | number) => {
        generated.push({
          candidate: `a=candidate:${
            foundationStart + generated.length
          } 1 UDP 100 ${ip} ${hostPort} typ host `,
          messageType: sourceCandidate.messageType,
          sdpMLineIndex: sourceCandidate.sdpMLineIndex,
          sdpMid: sourceCandidate.sdpMid,
        });
      };

      addHostCandidate(teredo.client4, teredo.udpPort);
      addHostCandidate(teredo.server4, candidatePort);
      addHostCandidate(teredo.client4, candidatePort);
      if (candidatePort !== XBOX_GAME_PORT) {
        addHostCandidate(teredo.server4, XBOX_GAME_PORT);
        addHostCandidate(teredo.client4, XBOX_GAME_PORT);
      }
    }

    if (generated.length > 0) {
      candidates.splice(sourceIndex + insertedCandidates + 1, 0, ...generated);
      insertedCandidates += generated.length;
    }
  });

  const endOfCandidates = candidates.filter(
    ({candidate}) => candidate === 'a=end-of-candidates',
  );
  const orderedCandidates = candidates.filter(
    ({candidate}) => candidate !== 'a=end-of-candidates',
  );

  if (!prioritizeIpv6) {
    return orderedCandidates
      .map((candidate, index) => ({candidate, index}))
      .sort((first, second) => {
        const firstIsLan = isPrivateLanIpv4Candidate(first.candidate.candidate);
        const secondIsLan = isPrivateLanIpv4Candidate(
          second.candidate.candidate,
        );
        if (firstIsLan === secondIsLan) {
          return first.index - second.index;
        }
        return firstIsLan ? -1 : 1;
      })
      .map(({candidate}) => candidate)
      .concat(endOfCandidates);
  }

  return orderedCandidates
    .map((candidate, index) => ({candidate, index}))
    .sort((first, second) => {
      const firstIp = first.candidate.candidate.trim().split(/\s+/)[4];
      const secondIp = second.candidate.candidate.trim().split(/\s+/)[4];
      const firstIsIpv6 = firstIp?.includes(':') ?? false;
      const secondIsIpv6 = secondIp?.includes(':') ?? false;

      if (firstIsIpv6 === secondIsIpv6) {
        return first.index - second.index;
      }
      return firstIsIpv6 ? -1 : 1;
    })
    .map(({candidate}) => candidate)
    .concat(endOfCandidates);
}
