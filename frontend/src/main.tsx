import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { App } from './App';
import { ThemeProvider } from './components/ThemeProvider';
import './index.css';
// Last-resort glyph fallback (see --font-sans in index.css) for symbols
// (arrows, dingbats, etc.) that many system UI fonts don't cover.
import '@fontsource/noto-sans-symbols/symbols-400.css';
import '@fontsource/noto-sans-symbols-2/symbols-400.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ThemeProvider>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </ThemeProvider>
  </React.StrictMode>,
);
