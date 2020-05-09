import glob
import os
import shutil
import sys

import cv2
import numpy as np
from skimage.measure import compare_ssim

dirname = '-images'
roi_window_name = 'ROI selection'

HSV_RED_SCOPES = [[[0, 120, 70], [10, 255, 255]], [[170, 120, 70], [180, 255, 255]]]
HSV_WHITE_SCOPES = [[[0, 0, 0], [0, 0, 255]], [[0, 0, 0], [0, 0, 255]]]
DEFAULT_HSV_SCOPE = HSV_RED_SCOPES


def process_video(video_name, roi, current_color_scopes):
    output_dir_name = os.path.splitext(video_name)[0] + dirname

    if os.path.exists(output_dir_name):
        shutil.rmtree(output_dir_name)

    os.mkdir(output_dir_name)

    cap = cv2.VideoCapture(video_name)

    i = 0
    previous_image = None
    is_writable = False

    while cap.isOpened():
        i += 1
        ret, frame = cap.read()

        if not ret:
            break

        crop_image = frame[int(roi[1]):int(roi[1] + roi[3]), int(roi[0]):int(roi[0] + roi[2])]

        hsv = cv2.cvtColor(crop_image, cv2.COLOR_BGR2HSV)

        result_color_mask = None

        for color_scope in current_color_scopes:
            lower_border = np.array(color_scope[0])
            upper_border = np.array(color_scope[1])

            color_mask = cv2.inRange(hsv, lower_border, upper_border)

            if result_color_mask is None:
                result_color_mask = color_mask
                continue

            result_color_mask += color_mask

        result_color_mask = cv2.morphologyEx(result_color_mask, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8), iterations=2)
        result_color_mask = cv2.dilate(result_color_mask, np.ones((3, 3), np.uint8), iterations=1)

        nb_components, output, stats, centroids = cv2.connectedComponentsWithStats(result_color_mask, connectivity=8)
        sizes = stats[1:, -1]

        if nb_components - 1 != 0:
            max_component_index = np.argmax(sizes) + 1
            image_with_max_component = np.zeros(output.shape)

            max_component_stat = stats[max_component_index]
            image_with_max_component[output == max_component_index] = 255

            if max_component_stat[4] > 20000 and (max_component_stat[2] / max_component_stat[3]) > 1 \
                    and max_component_stat[0] != 0 and max_component_stat[1] != 0 \
                    and max_component_stat[0] + max_component_stat[2] != result_color_mask.shape[1] \
                    and max_component_stat[1] + max_component_stat[3] != result_color_mask.shape[0]:

                if previous_image is None:
                    previous_image = result_color_mask
                    is_writable = True

                if is_writable or compare_ssim(result_color_mask, previous_image) < 0.8:
                    cv2.imwrite(os.path.join(output_dir_name, str(i) + '.jpg'), crop_image)
                    previous_image = result_color_mask
                    is_writable = False

        else:
            previous_image = None

    cap.release()


def main():
    is_first_iteration = True
    roi = None
    current_color_space_check = None

    if len(sys.argv) == 1:
        current_color_space_check = DEFAULT_HSV_SCOPE
    elif sys.argv[1] == 1:
        current_color_space_check = HSV_RED_SCOPES
    elif sys.argv[1] == 2:
        current_color_space_check = HSV_WHITE_SCOPES
    else:
        return

    for file in glob.glob("*.mp4"):
        if is_first_iteration:
            cap = cv2.VideoCapture(file)
            ret, frame = cap.read()

            aspect_ratio = 0.5

            width = int(frame.shape[1] * aspect_ratio)
            height = int(frame.shape[0] * aspect_ratio)

            resized = cv2.resize(frame, (width, height), interpolation=cv2.INTER_AREA)

            roi = cv2.selectROI(roi_window_name, resized)
            cv2.destroyWindow(roi_window_name)

            roi = [int(x / aspect_ratio) for x in roi]
            cap.release()

            is_first_iteration = False

        process_video(file, roi, current_color_space_check)

    cv2.destroyAllWindows()
    sys.exit()


if __name__ == "__main__":
    main()
