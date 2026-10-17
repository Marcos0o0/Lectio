/* Isolated EPUB pagination. Text offsets are independent of font and viewport size. */
(() => {
    const content = document.getElementById('lectio-content');
    const viewport = document.getElementById('lectio-viewport');
    const spacer = document.getElementById('lectio-spacer');
    let nodes = [], total = 0, page = 0, pages = 1, stride = 1, origin = 0;
    let anchor = 0, initialized = false, leaving = false, resizeTimer;

    function indexText() {
        nodes = []; total = 0;
        const walker = document.createTreeWalker(content, NodeFilter.SHOW_TEXT);
        while (walker.nextNode()) {
            const node = walker.currentNode;
            if (node.parentElement.closest('script,style') || !node.textContent.trim()) continue;
            nodes.push({node, start: total}); total += node.length;
        }
    }
    function rangeAt(offset) {
        if (!total) return null;
        offset = Math.max(0, Math.min(total - 1, offset));
        let low = 0, high = nodes.length - 1;
        while (low < high) {
            const mid = Math.ceil((low + high) / 2);
            if (nodes[mid].start <= offset) low = mid; else high = mid - 1;
        }
        const item = nodes[low], range = document.createRange();
        range.setStart(item.node, offset - item.start);
        range.setEnd(item.node, Math.min(item.node.length, offset - item.start + 1));
        return range;
    }
    function pageForRect(rect) {
        return Math.max(0, Math.min(pages - 1, Math.floor((rect.left + viewport.scrollLeft - origin + 1) / stride)));
    }
    function pageAt(offset) {
        const range = rangeAt(offset);
        return range ? pageForRect(range.getBoundingClientRect()) : 0;
    }
    function firstOffset(target) {
        if (target === 0) return 0;
        let low = 0, high = total;
        while (low < high) {
            const mid = Math.floor((low + high) / 2);
            if (pageAt(mid) < target) low = mid + 1; else high = mid;
        }
        return low;
    }
    function show(target, savedAnchor) {
        page = Math.max(0, Math.min(pages - 1, target));
        viewport.scrollLeft = page * stride;
        // Preserve the same text on reflow rather than accumulating rounding drift.
        anchor = savedAnchor == null ? firstOffset(page) : Math.max(0, Math.min(total, savedAnchor));
        const progress = page === pages - 1 ? 1 : total ? anchor / total : page / Math.max(1, pages - 1);
        Lectio.pagination(page, pages, total ? anchor : -1, progress);
    }
    function layout() {
        stride = innerWidth;
        const style = getComputedStyle(document.body);
        const width = document.body.clientWidth - parseFloat(style.paddingLeft) - parseFloat(style.paddingRight);
        origin = (innerWidth - document.body.clientWidth) / 2 + parseFloat(style.paddingLeft);
        content.style.left = origin + 'px';
        content.style.width = width + 'px';
        content.style.height = Math.max(40, innerHeight - parseFloat(style.paddingTop) - parseFloat(style.paddingBottom)) + 'px';
        content.style.columnWidth = width + 'px';
        content.style.columnGap = (stride - width) + 'px';
        pages = Math.max(1, Math.round((content.scrollWidth - width) / stride) + 1);
        spacer.style.width = (pages * stride) + 'px';
    }
    window.lectioTurn = direction => {
        if (!initialized || leaving || getSelection().toString().trim()) return;
        const target = page + (direction > 0 ? 1 : -1);
        if (target < 0 || target >= pages) {
            leaving = true; Lectio.boundary(direction > 0 ? 1 : -1);
            setTimeout(() => {leaving = false;}, 500);
        } else show(target);
    };
    window.lectioRestore = (progress, offset) => {
        if (!initialized) return;
        if (!total) show(Math.round(progress * (pages - 1)));
        else if (offset >= 0) show(pageAt(offset), offset);
        else show(progress >= 1 ? pages - 1 : pageAt(Math.floor(total * Math.max(0, progress))));
    };
    window.lectioFragment = id => {
        const element = document.getElementById(id);
        if (element && content.contains(element)) show(pageForRect(element.getBoundingClientRect()));
    };
    window.lectioSearch = (query, occurrence) => {
        content.querySelectorAll('mark[data-lectio-search]').forEach(mark => mark.replaceWith(...mark.childNodes));
        indexText();
        const text = nodes.map(item => item.node.textContent).join('').toLowerCase();
        query = query.toLowerCase();
        if (!query) return;
        let found = -1, start = 0;
        for (let i = 0; i <= occurrence; i++) {
            found = text.indexOf(query, start);
            if (found < 0) return;
            start = found + query.length;
        }
        const first = rangeAt(found), last = rangeAt(found + query.length - 1);
        if (!first || !last) return;
        const range = document.createRange();
        range.setStart(first.startContainer, first.startOffset);
        range.setEnd(last.endContainer, last.endOffset);
        const mark = document.createElement('mark'); mark.dataset.lectioSearch = 'true';
        mark.appendChild(range.extractContents()); range.insertNode(mark);
        indexText(); layout(); show(pageAt(found), found);
    };
    window.lectioInit = async (progress, offset, fragment, search, occurrence) => {
        await document.fonts.ready;
        // Images get an initial chance to settle; late loads keep the same text anchor.
        await Promise.race([Promise.all(Array.from(content.images || content.querySelectorAll('img')).map(img => img.complete ? Promise.resolve() : new Promise(resolve => {img.addEventListener('load', resolve, {once:true});img.addEventListener('error', resolve, {once:true});}))), new Promise(resolve => setTimeout(resolve, 1000))]);
        indexText(); layout(); initialized = true;
        window.lectioRestore(progress, offset);
        if (fragment) window.lectioFragment(fragment);
        if (search) window.lectioSearch(search, occurrence);
    };
    function reflow() {
        if (!initialized) return;
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(() => {const saved = anchor, fraction = page / Math.max(1, pages - 1); layout(); if (total) show(pageAt(saved), saved); else show(Math.round(fraction * (pages - 1)));}, 80);
    }
    window.addEventListener('resize', reflow);
    content.querySelectorAll('img').forEach(img => img.addEventListener('load', reflow));
    document.addEventListener('keydown', event => {
        if (['ArrowRight', 'PageDown', 'ArrowLeft', 'PageUp'].includes(event.key)) {
            event.preventDefault(); window.lectioTurn(['ArrowRight','PageDown'].includes(event.key) ? 1 : -1);
        }
    });
    // Tests/diagnostics read the actual engine; no separate test pagination exists.
    window.lectioPage = () => ({page, pages, anchor, total, stride});
})();
