import { beforeEach, describe, expect, it, vi } from 'vitest';
import { UVerifyClient } from '../UVerifyClient.js';
import { UVerifyApiError, UVerifyValidationError } from '../errors.js';
import type {
  CertificateResponse,
  BuildTransactionResponse,
  UserActionRequestResponse,
  ExecuteUserActionResponse,
} from '../types/index.js';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function mockFetch(status: number, body: unknown) {
  const text = body === undefined ? '' : JSON.stringify(body);
  return vi.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    statusText: status === 200 ? 'OK' : 'Error',
    text: () => Promise.resolve(text),
    json: () => Promise.resolve(body),
  });
}

const CERT: CertificateResponse = {
  hash: 'abc123',
  address: 'addr1test',
  blockHash: 'block001',
  blockNumber: 100,
  transactionHash: 'tx001',
  slot: 9000,
  creationTime: '2024-01-01T00:00:00Z',
};

const BUILD_RESPONSE: BuildTransactionResponse = {
  unsignedTransaction: 'cbor_unsigned',
  type: 'default',
  status: { message: 'OK', code: 200 },
};

const CHALLENGE: UserActionRequestResponse = {
  address: 'addr1test',
  action: 'USER_INFO',
  signature: 'server_sig',
  timestamp: 1700000000,
  message: 'please sign this',
  status: 200,
};

const EXECUTE_RESPONSE: ExecuteUserActionResponse = {
  status: 200,
  state: {
    id: 'state-1',
    owner: 'addr1test',
    fee: 1000000,
    feeInterval: 10,
    feeReceivers: [],
    ttl: 1800000000,
    countdown: 50,
    certificates: [],
    batchSize: 5,
    bootstrapDatumName: 'bootstrap',
    keepAsOracle: false,
  },
};

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('UVerifyClient', () => {
  beforeEach(() => {
    vi.unstubAllGlobals();
  });

  // -------------------------------------------------------------------------
  // Constructor & options
  // -------------------------------------------------------------------------

  describe('constructor', () => {
    it('uses the public API URL by default', async () => {
      const fetch = mockFetch(200, [CERT]);
      vi.stubGlobal('fetch', fetch);

      const client = new UVerifyClient();
      await client.verify('abc123');

      expect(fetch).toHaveBeenCalledWith(
        'https://api.uverify.io/api/v1/verify/abc123',
        expect.any(Object)
      );
    });

    it('respects a custom baseUrl and strips trailing slash', async () => {
      const fetch = mockFetch(200, [CERT]);
      vi.stubGlobal('fetch', fetch);

      const client = new UVerifyClient({ baseUrl: 'https://self-hosted.example.com/' });
      await client.verify('abc123');

      expect(fetch).toHaveBeenCalledWith(
        'https://self-hosted.example.com/api/v1/verify/abc123',
        expect.any(Object)
      );
    });

    it('merges custom headers into every request', async () => {
      const fetch = mockFetch(200, [CERT]);
      vi.stubGlobal('fetch', fetch);

      const client = new UVerifyClient({ headers: { 'X-Api-Key': 'secret' } });
      await client.verify('abc123');

      const init = fetch.mock.calls[0][1] as RequestInit;
      expect((init.headers as Record<string, string>)['X-Api-Key']).toBe('secret');
    });

    it('always sets Content-Type and Accept headers', async () => {
      const fetch = mockFetch(200, [CERT]);
      vi.stubGlobal('fetch', fetch);

      const client = new UVerifyClient();
      await client.verify('abc123');

      const init = fetch.mock.calls[0][1] as RequestInit;
      const headers = init.headers as Record<string, string>;
      expect(headers['Content-Type']).toBe('application/json');
      expect(headers['Accept']).toBe('application/json');
    });

    it('exposes a .core object with the four low-level methods', () => {
      const client = new UVerifyClient();
      expect(typeof client.core.buildTransaction).toBe('function');
      expect(typeof client.core.submitTransaction).toBe('function');
      expect(typeof client.core.requestUserAction).toBe('function');
      expect(typeof client.core.executeUserAction).toBe('function');
    });
  });

  // -------------------------------------------------------------------------
  // verify
  // -------------------------------------------------------------------------

  describe('verify', () => {
    it('calls GET /api/v1/verify/{hash} and returns certificates', async () => {
      const fetch = mockFetch(200, [CERT]);
      vi.stubGlobal('fetch', fetch);

      const client = new UVerifyClient();
      const result = await client.verify('abc123');

      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/verify/abc123'),
        expect.objectContaining({ method: 'GET' })
      );
      expect(result).toEqual([CERT]);
    });

    it('returns an empty array when the server returns an empty body', async () => {
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        statusText: 'OK',
        text: () => Promise.resolve(''),
        json: () => Promise.resolve(undefined),
      }));

      const client = new UVerifyClient();
      const result = await client.verify('abc123');
      expect(result).toBeUndefined(); // raw undefined — callers should handle empty
    });
  });

  // -------------------------------------------------------------------------
  // verifyByTransaction
  // -------------------------------------------------------------------------

  describe('verifyByTransaction', () => {
    it('calls GET /api/v1/verify/by-transaction-hash/{txHash}/{dataHash}', async () => {
      const fetch = mockFetch(200, CERT);
      vi.stubGlobal('fetch', fetch);

      const client = new UVerifyClient();
      const result = await client.verifyByTransaction('tx001', 'abc123');

      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/verify/by-transaction-hash/tx001/abc123'),
        expect.objectContaining({ method: 'GET' })
      );
      expect(result).toEqual(CERT);
    });
  });

  // -------------------------------------------------------------------------
  // Error handling
  // -------------------------------------------------------------------------

  describe('API errors', () => {
    it('throws UVerifyApiError on a 404 response', async () => {
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        ok: false,
        status: 404,
        statusText: 'Not Found',
        text: () => Promise.resolve('{"message":"not found"}'),
        json: () => Promise.resolve({ message: 'not found' }),
      }));

      const client = new UVerifyClient();
      await expect(client.verify('missing')).rejects.toThrow(UVerifyApiError);
    });

    it('sets statusCode on UVerifyApiError', async () => {
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error',
        text: () => Promise.resolve('Internal Server Error'),
        json: () => { throw new Error('not json'); },
      }));

      const client = new UVerifyClient();
      let caught: UVerifyApiError | undefined;
      try {
        await client.verify('bad');
      } catch (e) {
        caught = e as UVerifyApiError;
      }
      expect(caught?.statusCode).toBe(500);
    });

    it('includes the response body in UVerifyApiError', async () => {
      const body = { message: 'hash too short' };
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        ok: false,
        status: 400,
        statusText: 'Bad Request',
        text: () => Promise.resolve(JSON.stringify(body)),
        json: () => Promise.resolve(body),
      }));

      const client = new UVerifyClient();
      let caught: UVerifyApiError | undefined;
      try {
        await client.verify('x');
      } catch (e) {
        caught = e as UVerifyApiError;
      }
      expect(caught?.responseBody).toEqual(body);
    });
  });

  // -------------------------------------------------------------------------
  // client.core — low-level methods
  // -------------------------------------------------------------------------

  describe('core.buildTransaction', () => {
    it('POST /api/v1/transaction/build with the request body', async () => {
      const fetch = mockFetch(200, BUILD_RESPONSE);
      vi.stubGlobal('fetch', fetch);

      const client = new UVerifyClient();
      const result = await client.core.buildTransaction({
        type: 'default',
        address: 'addr1test',
        certificates: [{ hash: 'abc', algorithm: 'SHA-256' }],
      });

      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/transaction/build'),
        expect.objectContaining({ method: 'POST' })
      );
      const body = JSON.parse((fetch.mock.calls[0][1] as RequestInit).body as string);
      expect(body.address).toBe('addr1test');
      expect(result.unsignedTransaction).toBe('cbor_unsigned');
    });
  });

  describe('core.submitTransaction', () => {
    it('POST /api/v1/transaction/submit with transaction and witnessSet', async () => {
      const fetch = mockFetch(200, undefined);
      vi.stubGlobal('fetch', fetch);

      const client = new UVerifyClient();
      await client.core.submitTransaction('signed_cbor', 'witness_cbor');

      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/transaction/submit'),
        expect.objectContaining({ method: 'POST' })
      );
      const body = JSON.parse((fetch.mock.calls[0][1] as RequestInit).body as string);
      expect(body.transaction).toBe('signed_cbor');
      expect(body.witnessSet).toBe('witness_cbor');
    });

    it('omits witnessSet when not provided', async () => {
      const fetch = mockFetch(200, undefined);
      vi.stubGlobal('fetch', fetch);

      const client = new UVerifyClient();
      await client.core.submitTransaction('signed_cbor');

      const body = JSON.parse((fetch.mock.calls[0][1] as RequestInit).body as string);
      expect(body.witnessSet).toBeUndefined();
    });
  });

  describe('core.requestUserAction', () => {
    it('POST /api/v1/user/request/action with address and action', async () => {
      const fetch = mockFetch(200, CHALLENGE);
      vi.stubGlobal('fetch', fetch);

      const client = new UVerifyClient();
      const result = await client.core.requestUserAction({
        address: 'addr1test',
        action: 'USER_INFO',
      });

      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/user/request/action'),
        expect.objectContaining({ method: 'POST' })
      );
      expect(result.message).toBe('please sign this');
    });
  });

  describe('core.executeUserAction', () => {
    it('POST /api/v1/user/state/action with full request', async () => {
      const fetch = mockFetch(200, EXECUTE_RESPONSE);
      vi.stubGlobal('fetch', fetch);

      const client = new UVerifyClient();
      const result = await client.core.executeUserAction({
        address: 'addr1test',
        action: 'USER_INFO',
        message: 'please sign this',
        signature: 'server_sig',
        timestamp: 1700000000,
        userSignature: 'user_sig',
        userPublicKey: 'user_key',
      });

      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/user/state/action'),
        expect.objectContaining({ method: 'POST' })
      );
      expect(result.state?.countdown).toBe(50);
    });
  });

  // -------------------------------------------------------------------------
  // issueCertificates
  // -------------------------------------------------------------------------

  describe('issueCertificates', () => {
    it('builds, signs, and submits a transaction', async () => {
      const fetch = vi.fn()
        .mockResolvedValueOnce({
          ok: true, status: 200, statusText: 'OK',
          text: () => Promise.resolve(JSON.stringify(BUILD_RESPONSE)),
          json: () => Promise.resolve(BUILD_RESPONSE),
        })
        .mockResolvedValueOnce({
          ok: true, status: 200, statusText: 'OK',
          text: () => Promise.resolve(''),
          json: () => Promise.resolve(undefined),
        });
      vi.stubGlobal('fetch', fetch);

      const signTx = vi.fn().mockResolvedValue('witness_cbor');
      const client = new UVerifyClient();

      await client.issueCertificates(
        'addr1test',
        [{ hash: 'abc', algorithm: 'SHA-256' }],
        signTx
      );

      expect(signTx).toHaveBeenCalledWith('cbor_unsigned');
      expect(fetch).toHaveBeenCalledTimes(2);

      // Second call is submit — verify it carries the witness set
      const submitBody = JSON.parse((fetch.mock.calls[1][1] as RequestInit).body as string);
      expect(submitBody.transaction).toBe('cbor_unsigned');
      expect(submitBody.witnessSet).toBe('witness_cbor');
    });

    it('uses the constructor-level signTx when no per-call callback is given', async () => {
      const fetch = vi.fn()
        .mockResolvedValueOnce({
          ok: true, status: 200, statusText: 'OK',
          text: () => Promise.resolve(JSON.stringify(BUILD_RESPONSE)),
          json: () => Promise.resolve(BUILD_RESPONSE),
        })
        .mockResolvedValueOnce({
          ok: true, status: 200, statusText: 'OK',
          text: () => Promise.resolve(''),
          json: () => Promise.resolve(undefined),
        });
      vi.stubGlobal('fetch', fetch);

      const signTx = vi.fn().mockResolvedValue('witness_cbor');
      const client = new UVerifyClient({ signTx });

      await client.issueCertificates('addr1test', [{ hash: 'abc' }]);

      expect(signTx).toHaveBeenCalledWith('cbor_unsigned');
    });

    it('passes stateId when provided', async () => {
      const fetch = vi.fn()
        .mockResolvedValueOnce({
          ok: true, status: 200, statusText: 'OK',
          text: () => Promise.resolve(JSON.stringify(BUILD_RESPONSE)),
          json: () => Promise.resolve(BUILD_RESPONSE),
        })
        .mockResolvedValueOnce({
          ok: true, status: 200, statusText: 'OK',
          text: () => Promise.resolve(''),
          json: () => Promise.resolve(undefined),
        });
      vi.stubGlobal('fetch', fetch);

      const client = new UVerifyClient({ signTx: vi.fn().mockResolvedValue('w') });
      await client.issueCertificates('addr1test', [{ hash: 'abc' }], undefined, 'my-state');

      const buildBody = JSON.parse((fetch.mock.calls[0][1] as RequestInit).body as string);
      expect(buildBody.stateId).toBe('my-state');
    });

    it('throws UVerifyValidationError when no signTx callback is available', async () => {
      const client = new UVerifyClient();
      await expect(
        client.issueCertificates('addr1test', [{ hash: 'abc' }])
      ).rejects.toThrow(UVerifyValidationError);
    });
  });

  // -------------------------------------------------------------------------
  // getUserInfo
  // -------------------------------------------------------------------------

  describe('getUserInfo', () => {
    function setupTwoStepFetch() {
      return vi.fn()
        .mockResolvedValueOnce({
          ok: true, status: 200, statusText: 'OK',
          text: () => Promise.resolve(JSON.stringify(CHALLENGE)),
          json: () => Promise.resolve(CHALLENGE),
        })
        .mockResolvedValueOnce({
          ok: true, status: 200, statusText: 'OK',
          text: () => Promise.resolve(JSON.stringify(EXECUTE_RESPONSE)),
          json: () => Promise.resolve(EXECUTE_RESPONSE),
        });
    }

    it('runs the full two-step flow and returns the user state', async () => {
      vi.stubGlobal('fetch', setupTwoStepFetch());
      const signMessage = vi.fn().mockResolvedValue({ key: 'pub_key', signature: 'user_sig' });

      const client = new UVerifyClient();
      const state = await client.getUserInfo('addr1test', signMessage);

      expect(signMessage).toHaveBeenCalledWith('please sign this');
      expect(state?.countdown).toBe(50);
    });

    it('uses the constructor-level signMessage callback', async () => {
      vi.stubGlobal('fetch', setupTwoStepFetch());
      const signMessage = vi.fn().mockResolvedValue({ key: 'pub_key', signature: 'user_sig' });

      const client = new UVerifyClient({ signMessage });
      const state = await client.getUserInfo('addr1test');

      expect(signMessage).toHaveBeenCalled();
      expect(state?.id).toBe('state-1');
    });

    it('passes the server signature and user signature to executeUserAction', async () => {
      const fetch = setupTwoStepFetch();
      vi.stubGlobal('fetch', fetch);
      const signMessage = vi.fn().mockResolvedValue({ key: 'pub_key', signature: 'user_sig' });

      const client = new UVerifyClient();
      await client.getUserInfo('addr1test', signMessage);

      const executeBody = JSON.parse((fetch.mock.calls[1][1] as RequestInit).body as string);
      expect(executeBody.signature).toBe('server_sig');
      expect(executeBody.userSignature).toBe('user_sig');
      expect(executeBody.userPublicKey).toBe('pub_key');
      expect(executeBody.message).toBe('please sign this');
      expect(executeBody.timestamp).toBe(1700000000);
    });

    it('throws UVerifyValidationError when no signMessage callback is available', async () => {
      const client = new UVerifyClient();
      await expect(client.getUserInfo('addr1test')).rejects.toThrow(UVerifyValidationError);
    });
  });

  // -------------------------------------------------------------------------
  // invalidateState
  // -------------------------------------------------------------------------

  describe('invalidateState', () => {
    it('sends INVALIDATE_STATE action with stateId', async () => {
      const challenge: UserActionRequestResponse = { ...CHALLENGE, action: 'INVALIDATE_STATE' };
      const fetch = vi.fn()
        .mockResolvedValueOnce({
          ok: true, status: 200, statusText: 'OK',
          text: () => Promise.resolve(JSON.stringify(challenge)),
          json: () => Promise.resolve(challenge),
        })
        .mockResolvedValueOnce({
          ok: true, status: 200, statusText: 'OK',
          text: () => Promise.resolve(JSON.stringify({ status: 200 })),
          json: () => Promise.resolve({ status: 200 }),
        });
      vi.stubGlobal('fetch', fetch);

      const signMessage = vi.fn().mockResolvedValue({ key: 'k', signature: 's' });
      const client = new UVerifyClient();
      await client.invalidateState('addr1test', 'state-1', signMessage);

      const requestBody = JSON.parse((fetch.mock.calls[0][1] as RequestInit).body as string);
      expect(requestBody.action).toBe('INVALIDATE_STATE');
      expect(requestBody.stateId).toBe('state-1');
    });
  });

  // -------------------------------------------------------------------------
  // optOut
  // -------------------------------------------------------------------------

  describe('optOut', () => {
    it('sends OPT_OUT action with stateId', async () => {
      const challenge: UserActionRequestResponse = { ...CHALLENGE, action: 'OPT_OUT' };
      const fetch = vi.fn()
        .mockResolvedValueOnce({
          ok: true, status: 200, statusText: 'OK',
          text: () => Promise.resolve(JSON.stringify(challenge)),
          json: () => Promise.resolve(challenge),
        })
        .mockResolvedValueOnce({
          ok: true, status: 200, statusText: 'OK',
          text: () => Promise.resolve(JSON.stringify({ status: 200 })),
          json: () => Promise.resolve({ status: 200 }),
        });
      vi.stubGlobal('fetch', fetch);

      const signMessage = vi.fn().mockResolvedValue({ key: 'k', signature: 's' });
      const client = new UVerifyClient();
      await client.optOut('addr1test', 'state-1', signMessage);

      const requestBody = JSON.parse((fetch.mock.calls[0][1] as RequestInit).body as string);
      expect(requestBody.action).toBe('OPT_OUT');
      expect(requestBody.stateId).toBe('state-1');
    });
  });
});
