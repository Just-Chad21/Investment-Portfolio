import { csvExportUrl } from '../api/client';

// A plain link to the backend's CSV endpoint, not a fetch+blob dance — the browser
// handles the download natively using the filename from the server's
// Content-Disposition header (Phase 5). Whatever filters are active in the history
// table are passed through so the download matches what's on screen.
export default function CsvDownloadButton({ investorId, filters }) {
  return (
    <a className="csv-download-button" href={csvExportUrl(investorId, filters)}>
      <svg width="13" height="13" viewBox="0 0 16 16" fill="none" aria-hidden="true">
        <path
          d="M8 2v8m0 0 3-3m-3 3L5 7M3 12v1a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2v-1"
          stroke="currentColor"
          strokeWidth="1.4"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
      Download CSV
    </a>
  );
}
