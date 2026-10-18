/**
 * 历年真题在线预览（pdf.js 3.11.174 legacy 预构建组件）。
 *
 * 仅在线预览：本页刻意不提供下载、打印、打开原文件、分享入口。
 * 加载方式：viewer.html?file=<urlencode(F1 带签名地址)>，签名由后端 A2/D7 拼好。
 */
(function () {
  'use strict';

  var params = new URLSearchParams(window.location.search);
  var fileUrl = params.get('file');
  var errorBox = document.getElementById('error');
  var loadingBox = document.getElementById('loading');
  var prevButton = document.getElementById('prev');
  var nextButton = document.getElementById('next');
  var pageInfo = document.getElementById('pageInfo');
  var zoomInButton = document.getElementById('zoomIn');
  var zoomOutButton = document.getElementById('zoomOut');
  var fitWidthButton = document.getElementById('fitWidth');
  var zoomInfo = document.getElementById('zoomInfo');

  function showError(message) {
    if (loadingBox) {
      loadingBox.hidden = true;
    }
    if (errorBox) {
      errorBox.hidden = false;
      errorBox.textContent = message;
    }
  }

  if (!fileUrl) {
    showError('缺少文件地址，请从 App 或管理端重新打开预览。');
    return;
  }
  if (!window.pdfjsLib || !window.pdfjsViewer) {
    showError('PDF 组件加载失败，请刷新页面重试。');
    return;
  }

  pdfjsLib.GlobalWorkerOptions.workerSrc = './build/pdf.worker.js';

  var eventBus = new pdfjsViewer.EventBus();
  var linkService = new pdfjsViewer.PDFLinkService({ eventBus: eventBus });
  var viewer;

  try {
    viewer = new pdfjsViewer.PDFViewer({
      container: document.getElementById('viewerContainer'),
      viewer: document.getElementById('viewer'),
      eventBus: eventBus,
      linkService: linkService
    });
    linkService.setViewer(viewer);
  } catch (error) {
    showError('预览组件初始化失败：' + (error && error.message ? error.message : '未知错误'));
    return;
  }

  function refreshPageInfo() {
    var total = viewer.pagesCount || 0;
    var current = viewer.currentPageNumber || 0;
    pageInfo.textContent = total > 0 ? current + ' / ' + total : '- / -';
    prevButton.disabled = current <= 1;
    nextButton.disabled = total === 0 || current >= total;
  }

  function refreshZoomInfo() {
    var scale = viewer.currentScale || 1;
    zoomInfo.textContent = Math.round(scale * 100) + '%';
  }

  eventBus.on('pagesinit', function () {
    viewer.currentScaleValue = 'page-width';
    refreshPageInfo();
    refreshZoomInfo();
  });
  eventBus.on('pagechanging', refreshPageInfo);
  eventBus.on('scalechanging', refreshZoomInfo);

  prevButton.addEventListener('click', function () {
    if (viewer.currentPageNumber > 1) {
      viewer.currentPageNumber -= 1;
    }
  });
  nextButton.addEventListener('click', function () {
    if (viewer.currentPageNumber < viewer.pagesCount) {
      viewer.currentPageNumber += 1;
    }
  });
  zoomInButton.addEventListener('click', function () {
    viewer.increaseScale();
  });
  zoomOutButton.addEventListener('click', function () {
    viewer.decreaseScale();
  });
  fitWidthButton.addEventListener('click', function () {
    viewer.currentScaleValue = 'page-width';
  });

  pdfjsLib
    .getDocument({
      url: fileUrl,
      cMapUrl: './cmaps/',
      cMapPacked: true,
      standardFontDataUrl: './standard_fonts/',
      withCredentials: false
    })
    .promise.then(function (pdfDocument) {
      if (loadingBox) {
        loadingBox.hidden = true;
      }
      viewer.setDocument(pdfDocument);
      linkService.setDocument(pdfDocument, null);
      refreshPageInfo();
      refreshZoomInfo();
    })
    .catch(function (error) {
      var reason = error && error.message ? error.message : '未知错误';
      if (error && error.status === 403) {
        showError('预览链接已过期或无效，请返回重新打开。');
      } else if (error && error.status === 404) {
        showError('该真题已下架或被删除。');
      } else {
        showError('文件加载失败：' + reason);
      }
    });
})();
