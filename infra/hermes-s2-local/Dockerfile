FROM python:3.14.0-slim-bookworm
LABEL org.opencontainers.image.revision="4d04303818b61bf3aa09278af48356315515965b"
WORKDIR /opt/hermes
ADD .local/source.tar /opt/hermes/
ENV HERMES_RUNTIME_DIR=/opt/hermes/store
RUN python -m pm.build_env --source /opt/hermes --python /usr/local/bin/python \
    --out /opt/hermes/.venv --no-install-project --sealed --extra web
RUN mkdir -p /data /workspace && chown -R 10000:10000 /data /workspace
COPY entrypoint.py /opt/s2-entrypoint.py
ENV HOME=/data HERMES_HOME=/data/hermes PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
USER 10000:10000
ENTRYPOINT ["/opt/hermes/.venv/bin/python", "/opt/s2-entrypoint.py"]
