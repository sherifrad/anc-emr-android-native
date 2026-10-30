#!/bin/bash
# Double-click to run the MACA fix pipeline for the ANC project (about 20-60 minutes).
cd "$HOME/anc-emr-android-native" || exit 1
python3 .agents/pipeline/run_pipeline.py
echo; read -n 1 -s -r -p "Finished. Press any key to close"
